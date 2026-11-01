import argparse
import datetime
import json
import os
import statistics
import time
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path

PASTA = Path(__file__).parent
API = os.environ.get("SEUCORRE_API", "http://localhost:8080")
TIPOS_VALIDOS = {"REGENERATIVO", "INTERVALADO", "LONGO", "FARTLAKE"}
PERFIL = {
    "dadosFisicos": {"pesoKg": 68, "alturaCm": 170, "dataNascimento": "1997-03-10", "genero": "FEMININO",
                     "qualidadeSono": "BOA", "sedentario": False},
    "perfilAtleta": {"nivelCondicionamento": "INICIANTE", "objetivo": "COMPLETAR_5K", "jaCorre": False,
                     "diasDisponiveisSemana": 3, "diasSemanaTreino": "SEG,QUA,SEX"},
    "condicoesSaude": [],
}


def chamar(metodo, caminho, corpo=None, token=None):
    cabecalhos = {"content-type": "application/json"}
    if token:
        cabecalhos["authorization"] = f"Bearer {token}"
    dados = json.dumps(corpo).encode() if corpo is not None else None
    pedido = urllib.request.Request(API + caminho, data=dados, headers=cabecalhos, method=metodo)
    try:
        with urllib.request.urlopen(pedido, timeout=300) as resposta:
            texto = resposta.read().decode()
            return resposta.status, json.loads(texto) if texto else None
    except urllib.error.HTTPError as erro:
        texto = erro.read().decode()
        return erro.code, json.loads(texto) if texto.startswith("{") else texto


def volume(plano, semana):
    return round(sum(s.get("distanciaKm") or 0 for s in plano["sessoes"] if s["numeroSemana"] == semana), 2)


def respeita_dez_por_cento(plano):
    semanas = sorted({s["numeroSemana"] for s in plano["sessoes"]})
    return all(volume(plano, b) <= volume(plano, a) * 1.10 + 1e-9 for a, b in zip(semanas, semanas[1:]))


def criar_usuario_com_plano(caso_id):
    email = f"eval-{caso_id}-{int(time.time() * 1000)}@teste.local"
    chamar("POST", "/api/usuarios/registrar", {"nome": f"Eval {caso_id}", "email": email, "senha": "senha123"})
    _, login = chamar("POST", "/auth/login", {"email": email, "senha": "senha123"})
    token = login["token"]
    chamar("PUT", "/api/usuarios/me/onboarding", PERFIL, token)
    _, usuario = chamar("GET", "/api/usuarios/me", token=token)
    status, plano = chamar("POST", "/api/planos/gerar", {"objetivo": "COMPLETAR_5K"}, token)
    if status != 201:
        raise SystemExit(f"Falha ao gerar o plano base ({status}): {plano}")
    if "preset" not in (plano.get("resumoIA") or "").lower():
        raise SystemExit("O plano base não veio do preset. Suba o backend com TREINO_GERACAO_MODE=preset.")
    return token, usuario["id"], plano


def avaliar(caso, semana_atual):
    token, usuario_id, plano_base = criar_usuario_com_plano(caso["id"])
    checkin = {"usuarioId": usuario_id, "planoId": plano_base["id"], "semana": semana_atual,
               "dataCheckin": datetime.date.today().isoformat(), **caso["checkin"]}

    inicio = time.perf_counter()
    status, analise = chamar("POST", "/api/checkins", checkin, token)
    segundos = round(time.perf_counter() - inicio, 2)

    _, planos = chamar("GET", "/api/planos/me", token=token)
    plano_ativo = next((p for p in planos if p["status"] == "ATIVO"), None)
    reescrito = plano_ativo is not None and plano_ativo["id"] != plano_base["id"]
    volume_atual = volume(plano_base, semana_atual)
    volume_novo = volume(plano_ativo, 1) if reescrito else None

    verificacoes = {
        "sem_reescrita": status == 201 and not reescrito,
        "json_valido": status == 201 and reescrito,
        "tipos_validos": reescrito and all(s["tipo"] in TIPOS_VALIDOS for s in plano_ativo["sessoes"]),
        "dez_por_cento_bruto": None,
        "dez_por_cento_final": reescrito and respeita_dez_por_cento(plano_ativo),
        "nao_aumenta_volume": reescrito and volume_novo <= volume_atual,
        "reduz_volume_10": reescrito and volume_novo <= volume_atual * 0.90,
        "reduz_volume_30_sem_intervalado": reescrito and volume_novo <= volume_atual * 0.70 and all(
            s["tipo"] != "INTERVALADO" for s in plano_ativo["sessoes"] if s["numeroSemana"] == 1),
    }
    return {
        "id": caso["id"],
        "grupo": caso["grupo"],
        "status": status,
        "erro": None if status == 201 else analise,
        "riscoEsperado": caso["riscoEsperado"],
        "riscoObtido": analise.get("nivelRisco") if isinstance(analise, dict) else None,
        "segundos": segundos,
        "volumeSemanaAtual": volume_atual,
        "volumePrimeiraSemanaNova": volume_novo,
        "resultados": {nome: verificacoes[nome] for nome in caso["expectativas"]},
    }


def resumir(linhas):
    por_expectativa = defaultdict(lambda: [0, 0])
    for linha in linhas:
        for nome, passou in linha["resultados"].items():
            if passou is None:
                continue
            por_expectativa[nome][0] += int(passou)
            por_expectativa[nome][1] += 1

    print(f"\n{'expectativa':<34} passou")
    for nome, (passou, total) in sorted(por_expectativa.items()):
        print(f"{nome:<34} {passou}/{total} ({passou / total:.0%})")
    respondidas = [linha for linha in linhas if linha["status"] == 201]
    acertos = sum(linha["riscoObtido"] == linha["riscoEsperado"] for linha in respondidas)
    print(f"{'risco classificado certo':<34} {acertos}/{len(respondidas)}")
    tempos = [linha["segundos"] for linha in linhas if linha["status"] == 201]
    if tempos:
        print(f"{'tempo do check-in (mediana)':<34} {statistics.median(tempos):.1f} s (máx. {max(tempos):.1f} s)")
    erros = [linha for linha in linhas if linha["status"] != 201]
    for linha in erros:
        print(f"ERRO {linha['id']}: {linha['status']} {linha['erro']}")
    print("dez_por_cento_bruto: não medido nesta versão (a API só devolve o plano depois do guardrail).")


def main():
    argumentos = argparse.ArgumentParser(description="Eval do check-in do SeuCorre contra a API local.")
    argumentos.add_argument("--casos", help="ids separados por vírgula, ex.: c04,c13")
    argumentos.add_argument("--saida", default=str(PASTA / "resultados" / f"{datetime.date.today()}.jsonl"))
    argumentos.add_argument("--pausa", type=float, default=0, help="segundos de espera entre casos, para respeitar o limite por minuto")
    opcoes = argumentos.parse_args()

    definicao = json.loads((PASTA / "casos.json").read_text())
    casos = definicao["casos"]
    if opcoes.casos:
        escolhidos = set(opcoes.casos.split(","))
        casos = [caso for caso in casos if caso["id"] in escolhidos]

    saida = Path(opcoes.saida)
    saida.parent.mkdir(exist_ok=True)
    feitos = {json.loads(l)["id"] for l in saida.read_text().splitlines()} if saida.exists() else set()

    for caso in casos:
        if caso["id"] in feitos:
            continue
        linha = avaliar(caso, definicao["planoBase"]["semanaAtual"])
        if linha["status"] == 503:
            print(f"{caso['id']}: IA indisponível (cota do dia ou instabilidade). Parei; rode de novo depois para continuar.")
            break
        with saida.open("a") as arquivo:
            arquivo.write(json.dumps(linha, ensure_ascii=False) + "\n")
        marcas = " ".join(f"{n}={'ok' if v else ('-' if v is None else 'FALHOU')}" for n, v in linha["resultados"].items())
        print(f"{linha['id']} {linha['status']} risco={linha['riscoObtido']} {linha['segundos']}s {marcas}")
        time.sleep(opcoes.pausa)

    resumir([json.loads(l) for l in saida.read_text().splitlines()])


if __name__ == "__main__":
    main()

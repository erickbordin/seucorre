export function localIsoDate(date = new Date()) {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function currentWeekDates(today = new Date()) {
  return Array.from({ length: 7 }, (_, weekday) => {
    const date = new Date(today);
    date.setDate(today.getDate() - today.getDay() + weekday);
    return date;
  });
}

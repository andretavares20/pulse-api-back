package br.com.pulseapi.utils;

public class ScheduleIntervalConverter {

    public static Long convertToMilliseconds(String interval) {
        if (interval == null || interval.isEmpty()) {
            throw new IllegalArgumentException("Intervalo não pode ser nulo ou vazio");
        }

        try {
            String numberStr = interval.replaceAll("[^0-9.]", "");
            if (numberStr.isEmpty()) {
                throw new IllegalArgumentException("Número inválido no intervalo: " + interval);
            }

            double number = Double.parseDouble(numberStr);
            String unit = interval.replaceAll("[0-9.]", "").toLowerCase();

            switch (unit) {
                case "s":
                    return (long) (number * 1000); // Segundos para milissegundos
                case "m":
                    return (long) (number * 60 * 1000); // Minutos para milissegundos
                case "h":
                    return (long) (number * 60 * 60 * 1000); // Horas para milissegundos
                default:
                    throw new IllegalArgumentException("Unidade inválida: " + unit + ". Use 's' (segundos), 'm' (minutos) ou 'h' (horas)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de intervalo inválido: " + interval, e);
        }
    }

    public static String convertToString(Long milliseconds) {
        if (milliseconds == null) {
            throw new IllegalArgumentException("Milissegundos não podem ser nulos");
        }

        if (milliseconds < 0) {
            throw new IllegalArgumentException("Milissegundos não podem ser negativos");
        }

        // Converter para horas, minutos ou segundos
        if (milliseconds >= 60 * 60 * 1000) { // Mais de 1 hora
            long hours = milliseconds / (60 * 60 * 1000);
            return hours + "h";
        } else if (milliseconds >= 60 * 1000) { // Mais de 1 minuto
            long minutes = milliseconds / (60 * 1000);
            return minutes + "m";
        } else { // Menos de 1 minuto
            long seconds = milliseconds / 1000;
            return seconds + "s";
        }
    }
}

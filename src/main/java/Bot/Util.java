package Bot;

public class Util {

    public static void main(String[] args) {
        System.out.println(getDurationMS(15L));
        System.out.println(getDurationHMS(3650L));
    }

    public static String getDurationMS(Long value) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02d", value/3600));
        sb.append(":");
        sb.append(String.format("%02d", value % 3600));
        return sb.toString();
    }

    public static String getDurationHMS(Long value) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02d",value/3600));
        sb.append(":");
        sb.append(String.format("%02d", ((value % 3600) / 60)));
        sb.append(":");
        sb.append(String.format("%02d", ((value % 3600) % 60)));
        return sb.toString();
    }
}

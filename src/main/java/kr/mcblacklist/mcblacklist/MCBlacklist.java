package kr.mcblacklist.mcblacklist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by horyu on 2017-01-27.
 */
public class MCBlacklist {
    private static JavaPlugin plugin;

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
    }

    public static void check(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkPlayer(player));
    }

    private static void checkPlayer(Player player) {
        String uuid = null;
        if (Bukkit.getOnlineMode()) {
            try {
                uuid = player.getUniqueId().toString();
            } catch (IllegalStateException e) {
                // nothing
            }
        }

        if (uuid == null) {
            uuid = getUUIDFromMojangAPI(player.getName());
        }

        if (uuid != null) {
            String[] checkUUIDResult = checkWithType(uuid);
            if (checkUUIDResult[0].equals("DENY")) {
                kickPlayer(player, checkUUIDResult);
                return;
            }
        }

        String[] checkNameResult = checkWithType(player.getName());
        String[] checkIPResult = checkWithType(player.getAddress().getHostName());

        if (checkNameResult[0].equals("DENY") || checkIPResult[0].equals("DENY")) {
            kickPlayer(player, checkNameResult[0].equals("DENY") ? checkNameResult : checkIPResult);
        }
    }

    private static void kickPlayer(Player player, String[] result) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.kickPlayer("§c[MC-BlackList]\n§r" + result[2] + "\n§7문의 : http://mc-blacklist.kr/inquire"));
    }

    private static String[] checkWithType(String check) {
        String[] result = new String[4];
        try {
            URL url = new URL("http://api.mc-blacklist.kr/API/check/" + check);
            String jsonText = getJSONText(url);

            boolean error = jsonText.contains("\"error\":true");
            if (error) {
                String errorType = jsonText.split("\"error_type\":\"")[1].split("\"")[0];
                String errorMsg = decodeUnicode(jsonText.split("\"error_msg\":\"")[1].split("\"")[0]);

                result[0] = "ERROR";
                result[1] = errorType + " - " + errorMsg;

                return result;
            }

            boolean blacklist = jsonText.contains("\"blacklist\":true");
            if (blacklist) {
                final String date = jsonText.split("\"date\":\"")[1].split("\"")[0];
                final String reason = decodeUnicode(jsonText.split("\"reason\":\"")[1].split("\"")[0]);
                final String punisher = jsonText.split("\"punisher\":\"")[1].split("\"")[0];

                result[0] = "DENY";
                result[1] = date;
                result[2] = reason;
                result[3] = punisher;

                return result;
            }
        } catch (Exception ex) {
            result[0] = "ERROR";
            result[1] = ex.getMessage();
            return result;
        }

        result[0] = "PASS";
        return result;
    }

    private static String getUUIDFromMojangAPI(String nickName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + nickName + "?at=" + System.currentTimeMillis() / 1000);
            String jsonText = getJSONText(url);

            if (!jsonText.contains("\"id\":")) {
                return null;
            }

            String uuidWithNoDashes = jsonText.split("\"id\":\"")[1].split("\"")[0];
            return String.format("%s-%s-%s-%s-%s", uuidWithNoDashes.substring(0, 8), uuidWithNoDashes.substring(8, 12), uuidWithNoDashes.substring(12, 16), uuidWithNoDashes.substring(16, 20), uuidWithNoDashes.substring(20, 32));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getJSONText(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion());

            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = urlConnection.getInputStream();

            byte[] data = new byte[1024];
            int size;
            while ((size = inputStream.read(data)) != -1) {
                stringBuilder.append(new String(data, 0, size));
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            // nothing
        }
        return null;
    }

    private static String decodeUnicode(String unicode) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = unicode.indexOf("\\u"); i > -1; i = unicode.indexOf("\\u")) {
            char ch = (char) Integer.parseInt(unicode.substring(i + 2, i + 6), 16);
            stringBuilder.append(unicode.substring(0, i));
            stringBuilder.append(String.valueOf(ch));
            unicode = unicode.substring(i + 6);
        }

        stringBuilder.append(unicode);

        return stringBuilder.toString();
    }
}
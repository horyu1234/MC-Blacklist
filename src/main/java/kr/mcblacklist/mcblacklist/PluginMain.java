package kr.mcblacklist.mcblacklist;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by horyu on 2016-10-21.
 */
public class PluginMain extends JavaPlugin implements Listener {
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        MCBlacklist.init(this);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        MCBlacklist.check(event.getPlayer());
    }
}
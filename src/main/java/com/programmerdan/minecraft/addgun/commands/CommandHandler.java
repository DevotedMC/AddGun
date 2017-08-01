package com.programmerdan.minecraft.addgun.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.programmerdan.minecraft.addgun.AddGun;
import com.programmerdan.minecraft.addgun.guns.BasicGun;

public class CommandHandler implements CommandExecutor, TabCompleter {
	private AddGun plugin;
	
	private PluginCommand giveGun;
	
	public CommandHandler(FileConfiguration config) {
		plugin = AddGun.getPlugin();
		
		giveGun = plugin.getCommand("givegun");
		giveGun.setExecutor(this);
		giveGun.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String inv, String[] args) {
		if (cmd.equals(giveGun)) {
			if (args.length < 2) {// <player> <gun>
				return false;
			} else {
				Player target = Bukkit.getPlayer(args[0]); // player!
				if (target == null) {
					sender.sendMessage("Could not find player " + args[0]);
					return true;
				}
				BasicGun gun = plugin.getGun(args[1]);
				if (gun == null) {
					sender.sendMessage("Could not find gun " + args[1] + ". Try using tab-complete to search for valid guns.");
					return true;
				}
				target.getInventory().addItem(gun.getMinimalGun());
				sender.sendMessage("Gave gun " + args[1] + " to " + args[0]);
			}
		}
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String inv, String[] args) {
		if (cmd.equals(giveGun)) {
			if (args.length == 2) {
				if (args[1] == null || args[1].equals("")) {
					return new ArrayList<String>(plugin.getGuns());
				}
				Set<String> guns = plugin.getGuns();
				List<String> maybeGuns = new ArrayList<String>();
				for (String gun : guns) {
					if (gun.contains(args[1])) {
						maybeGuns.add(gun);
					}
				}
				return maybeGuns;
			} else if (args.length <= 1){
				String almost = (args.length == 1) ? args[0] : null;
				
				List<String> names = new ArrayList<String>();
				for (Player online : Bukkit.getOnlinePlayers()) {
					if (almost == null || almost.equals("") || online.getName().contains(almost)) {
						names.add(online.getName());
					}
				}
				return names;
			}
		}
		return null;
	}
}
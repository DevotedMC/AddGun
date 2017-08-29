package com.programmerdan.minecraft.addgun.events;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.HandlerList;

import com.programmerdan.minecraft.addgun.ammo.Bullet;
import com.programmerdan.minecraft.addgun.ammo.Clip;
import com.programmerdan.minecraft.addgun.guns.StandardGun;

public class LoadGunEvent extends GunEvent {
	private static final HandlerList handlers = new HandlerList();

	private final StandardGun gun;
	private final Clip clip;
	private final Bullet bullet;
	private final int rounds;
	private final HumanEntity player;
	
	public LoadGunEvent(StandardGun gun, Clip clip, Bullet bullet, int rounds, HumanEntity player) {
		super(false);
		this.gun = gun;
		this.clip = clip;
		this.bullet = bullet;
		this.rounds = rounds;
		this.player = player;
	}
	
	public StandardGun getGun() {
		return this.gun;
	}
	
	public Clip getClip() {
		return this.clip;
	}
	
	public Bullet getBullet() {
		return this.bullet;
	}
	
	public int getRounds() {
		return this.rounds;
	}
	
	public HumanEntity getHumanEntity() {
		return this.player;
	}
	
	@Override
	public HandlerList getHandlers() {
		return LoadGunEvent.handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}

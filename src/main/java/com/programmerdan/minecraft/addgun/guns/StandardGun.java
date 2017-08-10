package com.programmerdan.minecraft.addgun.guns;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValueAdapter;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import com.google.common.collect.Sets;
import com.programmerdan.minecraft.addgun.AddGun;
import com.programmerdan.minecraft.addgun.ArmorType;
import com.programmerdan.minecraft.addgun.ammo.AmmoType;
import com.programmerdan.minecraft.addgun.ammo.Bullet;
import com.programmerdan.minecraft.addgun.ammo.Clip;

import static com.programmerdan.minecraft.addgun.guns.Utilities.getArmorType;

public class StandardGun implements BasicGun {
	/**
	 * Is this gun enabled?
	 */
	private boolean enabled = false;

	/**
	 * Simplest example of the gun -- this is used for "matching" 
	 */
	private ItemStack gunExample = null;
	
	/**
	 * All the valid bullets that can be used with this gun, if it is directly loaded with bullets.
	 */
	private List<String> allBullets = null;
	
	/**
	 * All the valid clips that can be used with this gun, if it is loaded via clips.
	 */
	private List<String> allClips = null;
	
	/**
	 * max V in meters (blocks) / t -- this is gun's intrinsic, could be modified by bullets.
	 */
	private double maxSpeed = 200.0;
	
	/**
	 * min V in meters (blocks) / t
	 */
	private double minSpeed = 180.0;
	
	/**
	 * Internal avg V / t
	 */
	private double avgSpeed = (maxSpeed + minSpeed) / 2.0;
	
	/**
	 * Max times the gun can be fired before repair.
	 */
	private int maxUses = 500;
	
	/**
	 * Inflection point, when this many uses remain the risk of misfire is 50%
	 */
	private int middleRisk = 20;
	
	/**
	 * Misfire explosion change (percentage) -- could be modified by bullets
	 */
	private double misfireBlowoutChance = 0.05;
	
	/**
	 * Internal, computed. Will be gun item's max health / uses.
	 */
	private int damagePerUse = (int) (1562 / maxUses);
	

	
	/**
	 * When firing the gun and still / sneak isn't maximized, potentially angular jitter
	 */
	private double maxMissRadius = 30;
	/**
	 * Some guns are just innacurate .. minimum angular jitter regardless of stillness / sneak
	 */
	private double minMissRadius = 0;

	// dmg is from bullet
	
	/**
	 * or smack nerd over the head?
	 */
	private double bluntDamage = 3.0d;

	
	/**
	 * The unique identifier for this gun.
	 */
	private String name;
	
	/**
	 * Every gun has this tag somewhat hidden, it's used to quickly identify if a managed gun or not
	 */
	private String tag;
	
	/**
	 * Every fired bullet has this tag somewhat hidden, it's used to identify bullets. As is location tracks.
	 */
	private String bulletTag;
	
	/**
	 * Does this gun use clips, bullets, or autofeed from inventory to fire?
	 */
	private AmmoType ammoSource = AmmoType.INVENTORY;
	
	/**
	 * If BULLET, determines the max bullets that can be loaded into the weapon.
	 * CLIP is always 1, and INVENTORY is 0.
	 */
	private int maxAmmo = 0;
	
	/**
	 * Does this gun use XP to fire?
	 */
	private boolean usesXP = false;
	/**
	 * If the gun requires XP (energy) to fire... default no
	 */
	private int xpDraw = 0;
	
	
	/**
	 * Does this gun have a cooldown?
	 */
	private boolean hasCooldown = false;
	/**
	 * Should this gun enter into cooldown on equip (into hotbar)
	 */
	private boolean cooldownOnEquip = false;
	/**
	 * Cooldown between uses in ms
	 */
	private long cooldown = 500;

	/**
	 * This can be used to track any warnings sent to players and not resend
	 */
	private Map<UUID, String> warned = new ConcurrentHashMap<>();
	/**
	 * If the gun uses a cooldown, tracks which players are in cooldown and since when.
	 */
	private Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
	
	/**
	 * Should this gun be limited to a single gun in inventory?
	 */
	private boolean limitToOne = false;
	
	
	/**
	 * The point at which, after so many seconds of "stillness" -- head motion only -- that any aim impacts are halved.
	 */
	private double stillInflection = 7.5d;
	/**
	 * The point at which, after so many seconds of sneaking, any aim impacts are halved.
	 */
	private double sneakInflection = 7.5d;
	
	protected StandardGun(String name) {
		this.gunExample = this.generateGun();
		this.name = name;
		tag = ChatColor.BLACK + "Gun: "
				+ Integer.toHexString(this.getName().hashCode() + this.getName().length());
		bulletTag = ChatColor.BLACK + "Bullet: "
				+ Integer.toHexString(this.getName().hashCode() + this.getName().length());
	}
	
	public abstract ItemStack generateGun();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(double maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public double getMinSpeed() {
		return minSpeed;
	}

	public void setMinSpeed(double minSpeed) {
		this.minSpeed = minSpeed;
	}

	public int getMaxUses() {
		return maxUses;
	}

	public void setMaxUses(int maxUses) {
		this.maxUses = maxUses;
	}

	public int getMiddleRisk() {
		return middleRisk;
	}

	public void setMiddleRisk(int middleRisk) {
		this.middleRisk = middleRisk;
	}

	public double getMisfireBlowoutChance() {
		return misfireBlowoutChance;
	}

	public void setMisfireBlowoutChance(double misfireBlowoutChance) {
		this.misfireBlowoutChance = misfireBlowoutChance;
	}

	public int getDamagePerUse() {
		return damagePerUse;
	}

	public void setDamagePerUse(int damagePerUse) {
		this.damagePerUse = damagePerUse;
	}

	public int getXpDraw() {
		return xpDraw;
	}

	public void setXpDraw(int xpdraw) {
		this.xpDraw = xpdraw;
	}

	public double getMaxMissRadius() {
		return maxMissRadius;
	}

	public void setMaxMissRadius(double maxMissRadius) {
		this.maxMissRadius = maxMissRadius;
	}

	public double getMinMissRadius() {
		return minMissRadius;
	}

	public void setMinMissRadius(double minMissRadius) {
		this.minMissRadius = minMissRadius;
	}

	public double getBluntDamage() {
		return bluntDamage;
	}

	public void setBluntDamage(double bluntDamage) {
		this.bluntDamage = bluntDamage;
	}

	public boolean isUsesClips() {
		return AmmoType.CLIP.equals(this.ammoSource);
	}

	public boolean isUsesBullets() {
		return AmmoType.BULLET.equals(this.ammoSource);
	}

	public void setAmmoSource(AmmoType ammo) {
		this.ammoSource = ammo;
	}
	
	public AmmoType getAmmoSource() {
		return this.ammoSource;
	}

	public boolean isUsesXP() {
		return usesXP;
	}

	public void setUsesXP(boolean usesXP) {
		this.usesXP = usesXP;
	}

	public boolean isHasCooldown() {
		return hasCooldown;
	}

	public void setHasCooldown(boolean hasCooldown) {
		this.hasCooldown = hasCooldown;
	}

	public boolean isCooldownOnEquip() {
		return cooldownOnEquip;
	}

	public void setCooldownOnEquip(boolean cooldownOnEquip) {
		this.cooldownOnEquip = cooldownOnEquip;
	}

	public long getCooldown() {
		return cooldown;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public boolean isLimitToOne() {
		return limitToOne;
	}

	public void setLimitToOne(boolean limitToOne) {
		this.limitToOne = limitToOne;
	}

	public double getStillInflection() {
		return stillInflection;
	}

	public void setStillInflection(double stillInflection) {
		this.stillInflection = stillInflection;
	}

	public double getSneakInflection() {
		return sneakInflection;
	}

	public void setSneakInflection(double sneakInflection) {
		this.sneakInflection = sneakInflection;
	}

	public String getName() {
		return name;
	}
	
	/// NOW INTO GENERIC HANDLING CODE ///
	
	/**
	 * Subclasses are encouraged to override this.
	 * Just makes a sounds and spawns a particle. Note that if you're OK with this but still want
	 * to do some custom stuff on a nearmiss (perhaps jerk the entity away or something? I dunno)
	 * then consider using {@link #postMiss(HitDigest, Entity, Projectile, Projectile, Bullet)}
	 * instead, which gives you this same detail as well as the projectile spawned to "keep going"
	 * post-miss.
	 * 
	 * @param missData Data matrix showing miss
	 * @param missed What entity was almost hit
	 * @param bullet The bullet data
	 */
	public void nearMiss(HitDigest missData, Entity missed, Projectile bullet, Bullet type) {
		Location end = missData.hitLocation;
		World world = end.getWorld();
		world.playSound(end, Sound.BLOCK_GLASS_HIT, 1.0f, 1.5f);
		world.spawnParticle(Particle.SMOKE_NORMAL, end, 35);
	}
	
	/**
	 * Subclasses are encouraged to override this.
	 * Default behavior is to eject off of what you are riding, play a sound and spawn particle.
	 *
	 * 
	 * @param hitData Data matrix showing hit
	 * @param hit What entity was hit
	 * @param bullet The bullet data.
	 */
	public void preHit(HitDigest hitData, Entity hit, Projectile bullet, Bullet type) {
		Location end = hitData.hitLocation;
		World world = end.getWorld();
		hit.eject(); // eject the player
		hit.getPassengers().forEach(e -> e.eject()); // and ejects your passengers
		// make a new sound where it hits.
		world.playSound(end, Sound.ENTITY_FIREWORK_BLAST, 1.0f, 1.5f);
		// make a splash
		world.spawnParticle(Particle.SMOKE_NORMAL, end, 35);
	}
	
	/**
	 * Override this class, you can use it to provide particle effects along travel path. 
	 * 
	 * It is called after all other handling is done. Keep it lightweight
	 * 
	 * @param start The start location of flight
	 * @param end The end location of impact / miss
	 * @param type the type of bullet in play
	 * @param endOfFlight is the bullet still flying after this or has it impacted?
	 */
	public void flightPath(Location start, Location end, Bullet type, boolean endOfFlight) {
		// no special flight path stuff. maybe a whizzing sound?
	}
	
	/**
	 * Override this class, you use it to manage what happens when a non-Damageable is hit.
	 * 
	 * @param hitData the Data matrix showing hit information
	 * @param hit the non-damageable entity that was struck
	 * @param bullet The "Projectile" bullet doing the hitting
	 * @param bulletType The "Bullet" type of the projectile
	 */
	public void manageHit(HitDigest hitData, Entity hit, Projectile bullet, Bullet bulletType) {
		// do nothing in standard gun.
		Location end = hitData.hitLocation;
		World world = end.getWorld();
		world.playSound(end, Sound.BLOCK_GLASS_HIT, 1.0f, 1.5f);
		world.spawnParticle(Particle.SMOKE_NORMAL, end, 35);
	}

	/**
	 * Override this class, you use it to manage what happens when something damageable is hit.
	 * 
	 * @param hitData the Data matrix showing hit information
	 * @param hit the damageable entity that was struck
	 * @param bullet the "Projectile" bullet doing the hitting
	 * @param bulletType the "Bullet" type of the projectile
	 */
	public void manageDamage(HitDigest hitData, Damageable hit, Projectile bullet, Bullet bulletType) {
		// TODO: manage armor and armor bypass
		double median = bulletType.getAvgHitDamage(hitData.nearestHitPart);
		double twosigma = bulletType.getSpreadHitDamage(hitData.nearestHitPart);
		
		Vector speed = bullet.getVelocity();
		double absVelocity = speed.length();
		// speed is randomly uniform between min and max, we now approximate it into a gaussian distribution using a shaped sigmoid
		double variance = (absVelocity - (this.avgSpeed)) / ((this.maxSpeed - this.minSpeed + 1) / 4.0); // variance is now "centered" on average speed, in range [-avg, +avg]
		
		double baseRealDamage = (twosigma / 4) * (1 + (variance / Math.sqrt( 1 + (variance* variance)))) + median;

		double finalDamage = baseRealDamage;
		ItemStack armorHit = null;
		ItemStack shield = null;
		double shieldEffectiveness = 0.0d;
		
		if (hit instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) hit;
			EntityEquipment equipment = living.getEquipment();
			// now reductions?	
			switch(hitData.nearestHitPart) {
			case BODY:
			case CHEST_PLATE:
			case LEFT_ARM:
			case LEFT_FOOT:
			case LEFT_HAND:
			case RIGHT_ARM:
			case RIGHT_FOOT:
			case RIGHT_HAND:// all variants on body atm
				armorHit = equipment.getLeggings();
				shield = equipment.getItemInOffHand();
				break;
			case BOOTS:
			case FEET: // just feet
				armorHit = equipment.getBoots();
				break;
			case HEAD:
			case HELMET: // just head
				armorHit = equipment.getHelmet();
				shield = equipment.getItemInOffHand();
				break;
			case LEGGINGS:
			case LEFT_LEG:
			case RIGHT_LEG: 
			case LEGS: // just legs
				armorHit = equipment.getLeggings();
				break;
			case MISS: // no hit?
				nearMiss(hitData, hit, bullet, bulletType);
				break;
			default:
				break;
			}
			
			if (shield != null && Material.SHIELD.equals(shield.getType())) {
				if (hit instanceof HumanEntity) {
					HumanEntity human = (HumanEntity) hit;
					if (human.isBlocking()) {
						shieldEffectiveness = 1.0d;
					} else if (human.isHandRaised()) {
						shieldEffectiveness = 0.5d;
					}
				}
			} else {
				shield = null;
			}
		}
		
		if (armorHit != null) {
			// check bypass?
			
			// check armor type
			ArmorType grade = getArmorType(armorHit.getType()); 
			int protLevel = armorHit.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
			int projLevel = armorHit.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE);
			
			// TODO: calculate
		}
		
		if (shield != null) {
			// check bypass?
			double angle = Math.toDegrees(hit.getLocation().getDirection().angle(speed));
			// TODO?!?! check this
			if (angle >= -135 && angle < 135) {
				// no deflection!
				shieldEffectiveness = 0.0d;
			}
			
			finalDamage *= (1.0 - shieldEffectiveness);
		}
		
		//TODO: player states? custom shit? event?
		
		hit.damage(finalDamage, bullet);
	}
	


	/**
	 * Any post-hit cleanup can be handled here. This would be stuff not associated with manageHit or manageDamage.
	 * 
	 *  Provided as a handy hook, but fine to leave as no-op.
	 *  
	 * @param hitData the Data matrix describing hit information
	 * @param hit the Entity that was hit, after handling manageHit
	 * @param bullet the "Projectile" bullet doing the hitting
	 * @param bulletType the "Bullet" type of the projectile.
	 */
	abstract void postHit(HitDigest hitData, Entity hit, Projectile bullet, Bullet bulletType);

	/**
	 * Any post-miss cleanup can be handled here. Misses will automatically run this function, and
	 * it includes pre- and post-miss data.
	 * 
	 * @param hitData the Data matrix describing miss information
	 * @param hit the Entity that was missed, after miss management and continue projectile spawn.
	 * @param bullet the "Projectile" bullet doing the original missing
	 * @param continueBullet Since the original projectile is removed, this is a "continue" bullet spawned _after_ the miss entity.
	 * @param bulletType the "Bullet" type of the projectiles.
	 */
	abstract void postMiss(HitDigest hitData, Entity hit, Projectile bullet, Projectile continueBullet,
			Bullet bulletType);
	
	
	/**
	 * A basic shoot method, it _can_ be overridden but take care.
	 * Handles passing the bullet to its BulletType for configuration, sets shooter, velocity, etc.
	 * 
	 * @param begin The location to shoot from
	 * @param bulletType the Bullet type of this bullet
	 * @param shooter the entity shooting
	 * @param velocity the velocity to use as base for this shooting, if any
	 * @param overrideVelocity if true, use the passed velocity and override anything set up by the bullet type.
	 * @return the new Projectile that has been unleashed.
	 */
	public Projectile shoot(Location begin, Bullet bulletType, ProjectileSource shooter, Vector velocity, boolean overrideVelocity) {
		World world = begin.getWorld();
		Projectile newBullet = world.spawn(begin, bulletType.getProjectileType() );
		newBullet.setCustomName(this.bulletTag);
		newBullet.setBounce(false);
		newBullet.setGravity(true);
		newBullet.setShooter(newBullet.getShooter());
		
		bulletType.configureBullet(newBullet, world, shooter, velocity);
		
		if (overrideVelocity) {
			newBullet.setVelocity(velocity);
		}

		travelPaths.put(newBullet.getUniqueId(), begin);
		inFlightBullets.put(newBullet.getUniqueId(), bulletType);
		
		return newBullet;
	}

	/* Shooting helpers */
	
	/**
	 * This computes if enough XP is present to fire the gun and bullet that's chambered
	 * 
	 * @param entity the shooter
	 * @param bullet the Bullet type that's chambered
	 * @return true if enough fuel, false otherwise
	 */
	boolean hasFuel(LivingEntity entity, Bullet bullet) {
		if (!this.usesXP && bullet.getUsesXP()) return true; // no xp needs.
		
		int totalDraw = this.xpDraw + bullet.getXPDraw();
		
		int xpNeeds = computeTotalXP(entity) - totalDraw; 

		if (xpNeeds < 0 && (xpNeeds + AddGun.getPlugin().getXpPerBottle() * getInvXp(entity) < 0)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the proferred item is a gun.
	 * 
	 * @param toCheck the item to check if this gun
	 * @return true if gun, false otherwise
	 */
	@Override	
	public boolean isGun(ItemStack toCheck) {
		if (!enabled || toCheck == null)
			return false;

		if (!gunExample.getType().equals(toCheck.getType()))
			return false;

		if (!toCheck.hasItemMeta())
			return false;

		ItemMeta meta = toCheck.getItemMeta();

		if (meta.getLore().contains(tag))
			return true;

		return false;
	}
	
	/**
	 * Checks if this gun has enough health to still fire
	 * 
	 * @param toCheck Does _not_ confirm if gun, just check durability.
	 * @return true if alive, false otherwise.
	 */
	private boolean isAlive(ItemStack toCheck) {
		if (toCheck.getDurability() < (gunExample.getType().getMaxDurability() - (this.maxUses * damagePerUse)))
			return true;
		return false;
	}
	
	/**
	 * Checks if this gun has bullets / clips with bullets
	 * 
	 * @param toCheck Does _not_ confirm if gun, just checks for ammo signals.
	 * @return true if ammo found, false otherwise. Does not check if ammo works or not.
	 */
	public boolean isLoaded(ItemStack toCheck) {
		if (!toCheck.hasItemMeta())
			return false;
		
		ItemMeta meta = toCheck.getItemMeta();
		
		if (meta.getLore().contains("Bullets Loaded:")) {
			Bullet bulletType = Bullets.getBulletsFromMeta(meta);
			return bulletType != null;
		} else if (meta.getLore().contains("Clip loaded:")) {
			Clip clip = Clip.getClipFromMeta(meta);
			return clip.hasBullets();
		}
		return false;
	}

	/**
	 * Returns the unique imprint put on every bullet by this gun.
	 * 
	 * @return the imprint
	 */
	public String getBulletTag() {
		return this.bulletTag;
	}
	
	/**
	 * Returns the unique imprint put on every gun.
	 * 
	 * @return the imprint
	 */
	public String getGunTag() {
		return this.tag;
	}
}

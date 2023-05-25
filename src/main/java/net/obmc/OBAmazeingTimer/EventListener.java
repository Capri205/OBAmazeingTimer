package net.obmc.OBAmazeingTimer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.Material;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EventListener implements Listener
{
	static Logger log = Logger.getLogger("Minecraft");
	private String logmsgprefix = null;

	public EventListener() {
		
		logmsgprefix = OBAmazeingTimer.getLogMsgPrefix();

	}
	/*
	 * Detect a players movement and see if they have triggered a maze timer stop/start event
	 * Look at the location of the player and see if that matches the triggers from the config
	 * 
	 * @param {@link PlayerMoveEvent}
	 */
	@EventHandler
	public void onPlayerMove( PlayerMoveEvent event ) {
		
		Player player = event.getPlayer();
		Location playerloc = player.getLocation();
		MazeTrigger entryloc = OBAmazeingTimer.getInstance().getStartTimerTrigger();
		MazeTrigger exitloc = OBAmazeingTimer.getInstance().getStopTimerTrigger();
		
		// Entering maze - only process event if there's no timer running for a player
		if ( !OBAmazeingTimer.getInstance().getMazeTimer().timerRunning( player.getUniqueId() ) ) {
			
			// check if we've moved to the entry trigger block
			if ((int)playerloc.getX() == entryloc.getPadLocationX() &&
				(int)playerloc.getY() == entryloc.getPadLocationY() &&
				(int)playerloc.getZ() == entryloc.getPadLocationZ() &&
				playerloc.getBlock().getType().equals( entryloc.getPadMaterial() ) ) {
				
				log.log(Level.INFO, logmsgprefix + "Maze entry detected for " + player.getName() + "!" );

				// yes, so start a timer and task for the player
				OBAmazeingTimer.getInstance().getMazeTimer().StartTimer( player.getUniqueId() );
				
				//TODO: do something?
				//player.playSound(player.getLocation(), OBAmazeingTimer.getInstance().getSound(), 1.0f, 1.0f);
				//displayEffect(OBAmazeingTimer.getInstance().getEffect(), player.getLocation(), 1.0f, 1.0f, 1.0f, 1.0f, OBAmazeingTimer.getInstance().getParticleCount());
			}
		}
		
		// Exit maze - checks when a timer is running for a player
		if ( OBAmazeingTimer.getInstance().getMazeTimer().timerRunning( player.getUniqueId() ) ) {

			// check if we've exited the maze - ignore if not activated
			if ((int)playerloc.getX() == exitloc.getPadLocationX() &&
				(int)playerloc.getY() == exitloc.getPadLocationY() &&
				(int)playerloc.getZ() == exitloc.getPadLocationZ() &&
				playerloc.getBlock().getType().equals( exitloc.getPadMaterial() ) ) {
				
				log.log(Level.INFO, logmsgprefix + "Maze exit detected for " + player.getName() + "!" );

				OBAmazeingTimer.getInstance().getMazeTimer().StopTimerOnExit( player.getUniqueId() );
				
				//TODO: do sometthing?
				//player.playSound(player.getLocation(), OBAmazeingTimer.getInstance().getSound(), 1.0f, 1.0f);
				//displayEffect(OBAmazeingTimer.getInstance().getEffect(), player.getLocation(), 1.0f, 1.0f, 1.0f, 1.0f, OBAmazeingTimer.getInstance().getParticleCount());
			}
			
			// check if player has somehow moved outside of maze boundaries whilst being timed and cancel timer if they have
			// we check the X and Z coordinates of our corners and high and low Y coordinates to form essentially a box
			if ((int)playerloc.getX() > (int)OBAmazeingTimer.getInstance().getMazeSideHigh().getX() || (int)playerloc.getX() < (int)OBAmazeingTimer.getInstance().getMazeSideLow().getX() ||
				(int)playerloc.getZ() > (int)OBAmazeingTimer.getInstance().getMazeSideHigh().getZ() || (int)playerloc.getZ() < (int)OBAmazeingTimer.getInstance().getMazeSideLow().getZ() ||
			    (int)playerloc.getY() >= (int)OBAmazeingTimer.getInstance().getMazeHigh() || (int)playerloc.getY() < (int)OBAmazeingTimer.getInstance().getMazeLow()) {
				
				log.log(Level.INFO, logmsgprefix + "Out of boundary detected for " + player.getName() + "!" );
		
			    OBAmazeingTimer.getInstance().getMazeTimer().StopTimerOutBounds( player.getUniqueId() );
		     }
		}
		
	}

	/**
	 * See if the player has interacted in some way with our entry or exit place and cancel the event
	 * 
	 * @param {@link PlayerInteractEvent}
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Location playerloc = player.getLocation();
		
		MazeTrigger entryloc = OBAmazeingTimer.getInstance().getStartTimerTrigger();
		MazeTrigger exitloc = OBAmazeingTimer.getInstance().getStopTimerTrigger();
		
		if ( event.getAction() == Action.PHYSICAL ) {

			Material eventplate = event.getClickedBlock().getLocation().getBlock().getType();

			// check for entry plate event and cancel event
			if (!OBAmazeingTimer.getInstance().getMazeTimer().timerRunning( player.getUniqueId() ) &&
				(int)playerloc.getX() == entryloc.getPadLocationX() &&
				(int)playerloc.getY() == entryloc.getPadLocationY() &&
				(int)playerloc.getZ() == entryloc.getPadLocationZ() &&
				eventplate.equals( OBAmazeingTimer.getInstance().getStartTimerTrigger().getPadMaterial() ) ) {
				event.setCancelled(true);
			}
			
			// check for exit plate event and cancel
			if (OBAmazeingTimer.getInstance().getMazeTimer().timerRunning( player.getUniqueId() ) &&
				(int)playerloc.getX() == exitloc.getPadLocationX() &&
				(int)playerloc.getY() == exitloc.getPadLocationY() &&
				(int)playerloc.getZ() == exitloc.getPadLocationZ() &&
				eventplate.equals( OBAmazeingTimer.getInstance().getStopTimerTrigger().getPadMaterial() ) ) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	/*
	 * Detect if a player has quit whilst being timed and cancel their timer and task
	 * 
	 * @param {@link PlayerQuitEvent}
	 */
	public void onPlayerQuit( PlayerQuitEvent event ) {
		
		clearTaskTimer( event.getPlayer() );
	}
	
	/*
	 * Detect if a player has been kicked whilst being timed and cancel their timer and task
	 * 
	 * @param {@link PlayerKickEvent}
	 */
	@EventHandler
	public void onPlayerKick( PlayerKickEvent event ) {
		
		clearTaskTimer( event.getPlayer() );
	}

	/*
	 * Detect if a player joins and cancel any tasks that might be left over
	 * 
	 * @param {@link PlayerJoinEvent}
	 */
	@EventHandler
	public void onPlayerJoin( PlayerJoinEvent event ) {

		clearTaskTimer( event.getPlayer() );
	}

	/*
	 * Detect if a player respawns and cancel any timers and tasks
	 * 
	 * @param {@link PlayerRespawnEvent}
	 */
	@EventHandler
	public void onReSpawn( PlayerRespawnEvent event ) {

		clearTaskTimer( event.getPlayer() );
	}
	
	/*
	 * Detect if a player leaves the world and cancel any timers and tasks
	 * 
	 * @param {@link PlayerChangedWorldEvent}
	 */
	@EventHandler
	public void onWorldChange( PlayerChangedWorldEvent event ) {

		clearTaskTimer( event.getPlayer() );
	}

	/*
	 * Detect if a player dies and cancel any timers and tasks
	 * 
	 * @param {@link PlayerDeathEvent}
	 */
	@EventHandler
	public void onPlayerDeath( PlayerDeathEvent event ) {

		clearTaskTimer( event.getEntity() );
	}
	
	/*
	 * Remove a player timer and cancel any timer tasks
	 * 
	 * @param {@link Player}
	 */
	private void clearTaskTimer( Player player ) {
		
		if ( OBAmazeingTimer.getInstance().getMazeTimer().timerRunning( player.getUniqueId() ) ) {
			
			OBAmazeingTimer.getInstance().getMazeTimer().removePlayerTimer( player.getUniqueId() );
			OBAmazeingTimer.getInstance().getMazeTimer().removePlayerTask( player.getUniqueId() );
		}
	}
}

package net.obmc.OBAmazeingTimer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus.Internal;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class MazeTimer {
	
	static Logger log = Logger.getLogger("Minecraft");

	private Map<UUID, StopWatch> playertimers = new HashMap<UUID, StopWatch>();
	private Map<UUID, Integer> playertasks = new HashMap<UUID, Integer>();
	
	private String chatmsgprefix = null;
	private String logmsgprefix = null;
	
	/*
	 * Construct a new {@link MazeTimer} manager which will manage stopwatch timers for players
	 * 
	 * There are two lists. One contains stop watches for players and the other the task
	 * ID of any scheduler tasks associated with a [running] timer
	 */
	
	@Internal
	public MazeTimer() {
		
		chatmsgprefix = OBAmazeingTimer.getChatMsgPrefix();
		logmsgprefix = OBAmazeingTimer.getLogMsgPrefix();
	}
	
	/*
	 * Start a timer for a player
	 * 
	 * @param uuid of the player
	 */
	public void StartTimer( UUID playerid ) {
		
		Player player = Bukkit.getPlayer(playerid);

		// return if player already has a timer running
		if ( playertimers.containsKey( playerid ) && playertimers.get( playerid ).isStarted() ) {
			return;
		}
		
		if ( !playertimers.containsKey( playerid ) ) {
			playertimers.put( playerid, new StopWatch() );
		}

		playertimers.get( playerid ).reset();
		playertimers.get( playerid ).start();
		
		player.sendMessage( OBAmazeingTimer.getChatMsgPrefix() + "Started A-Maze-Ing maze timer for " + player.getName() + "!" );

		// run a task to update the player timer in the action bar
		updatePlayerTimer( playerid );
	}

	/*
	 * Stop a player timer and remove from our lists
	 * 
	 * @param the player UUID
	 */
	public void StopTimerOutBounds( UUID playerid ) {
		
		if ( playertimers.containsKey( playerid ) && playertimers.get( playerid ).isStarted() ) {
			
			//playertimers.get( playerid ).stop();
		    OBAmazeingTimer.getInstance().getMazeTimer().removePlayerTimer( playerid );
		}
	}
	
	/*
	 *  Process events for player exiting the maze by stepping on the exit trigger
	 *  Stops the player timer, send messages, fanfare and add to leaderboard if eligible
	 *  and redraws the leaderboard
	 *  
	 *  @param UUID of player
	 */
	public void StopTimerOnExit( UUID playerid ) {
		
		Player player = Bukkit.getPlayer( playerid );
		
		if ( !playertimers.containsKey( playerid ) || playertimers.get( playerid ).isStopped() ) {
			return;
		}
		
		// stop timer and get a formatted time
		playertimers.get( playerid ).stop();
		Long finaltime = playertimers.get( playerid ).getTime();
		String finaltimestr = Utils.formatTime( finaltime, "final" );
		log.log(Level.INFO, logmsgprefix + "Stopped maze timer for " + player.getName() + " at " + finaltimestr + "(" + finaltime + "ms)" );

		// send a title message with the formatted time
		player.sendTitle(ChatColor.GOLD + " Congratulations!", ChatColor.YELLOW + "Your final time was " + finaltimestr, 1*20, 5*20, 2*20);
		
		// Get rank for time and add to the leaderboard if eligible and report if player beat their time or not
		LeaderboardManager leaderboardmgr = OBAmazeingTimer.getInstance().getLeaderboardManager();
		Long playercurrenttime = leaderboardmgr.getPlayerExistingTime( playerid );

		int timerank = leaderboardmgr.getRankForTime( finaltime );
		if ( timerank <= ( 3 * 4 ) ) {

			// set flags to determine which message to send
			boolean beatexistingtime = false;
			boolean hasexistingtime = playercurrenttime != null ? true : false;
			if ( hasexistingtime && finaltime < playercurrenttime ) {
				beatexistingtime = true;
			}
			boolean failedtobeat = ( hasexistingtime && !beatexistingtime ) ? true : false;
				
			// first place
			if ( timerank == 1 ) {
				
				player.sendMessage( chatmsgprefix + ChatColor.GOLD + "Outstanding!! " + ChatColor.GREEN + "You made it to FIRST place on the leaderboard!! " + ChatColor.LIGHT_PURPLE + "WOW!!" );
				leaderboardmgr.addTime( finaltime, playerid );
				//TODO: mega fanfare
			}
			
			// second to fourth place
			if ( timerank > 1 && timerank <= 4 && !failedtobeat ) {

				player.sendMessage( chatmsgprefix + ChatColor.GOLD + "Brilliant!! " + ChatColor.GREEN + "You made it into the top 4" + ( beatexistingtime ? " and beat your existing time!" : "! Way to go!" ) );
				if ( beatexistingtime ) {
					player.sendMessage( chatmsgprefix +  ChatColor.GREEN + "You beat your old time by " + Utils.formatTime( ( playercurrenttime - finaltime ), "final" ) + ". Incredible!!" );
				}
				leaderboardmgr.addTime( finaltime, playerid );
				//TODO: fanfare
			}
			
			// somewhere on the displayed leaderboard
			if ( timerank > 4 && !failedtobeat ) {
				
				player.sendMessage( chatmsgprefix +  ChatColor.GOLD + "Great job! " + ChatColor.GREEN + "you ranked #" + timerank + " on the leaderboard" + ( beatexistingtime ? " and beat your existing time!" : "! Great job!" ) );
				
				// if they beat their old time, show by how much
				if ( beatexistingtime ) {
					player.sendMessage( chatmsgprefix +  ChatColor.GREEN + "You beat your old time by " + Utils.formatTime( ( playercurrenttime - finaltime ), "final" ) + ". Way to go!!" );
				}
				leaderboardmgr.addTime( finaltime, playerid );
			}

			// failed to beat an existing time
			if ( failedtobeat ) {
				
				player.sendMessage( chatmsgprefix + ChatColor.GOLD + "Good attempt! " + ChatColor.GREEN + "However you failed to beat your existing time." );
				// did not beat existing score - give some encouragement if close - 10% and 20% range for now
				double timediff = 1.0 - playercurrenttime.doubleValue() / finaltime.doubleValue();
				if (  timediff < 0.1 ) {
					player.sendMessage( chatmsgprefix + ChatColor.GREEN + "Getting very close! Keep trying as you're almost there!!" );
				} else if ( timediff < 0.2 ) {
					player.sendMessage( chatmsgprefix + ChatColor.GREEN + "Getting closer! Get those neurons fired up and remember the route!!" );
				}
			}

			// redraw leaderboard
			OBAmazeingTimer.getInstance().getSignManager().populateSigns( OBAmazeingTimer.getInstance().getLeaderboardManager() );

		} else {
			
			// not on leaderboard with this time
			player.sendMessage( chatmsgprefix + ChatColor.GOLD + "Nice try! " + ChatColor.GREEN + "However you didn't make it onto the leaderboard with this time!" );
			player.sendMessage( chatmsgprefix + ChatColor.GREEN + "Better luck next try!" );
		}

		// redraw leaderboard and remove any timers and tasks
		OBAmazeingTimer.getInstance().getSignManager().populateSigns( OBAmazeingTimer.getInstance().getLeaderboardManager() );
		removePlayerTimer( playerid );
		removePlayerTask( playerid );

	}
	
	/*
	 * Check whether a player has a timer running or not
	 * 
	 * @param UUID of the player
	 * @return true if a timer is running, false if not
	 */
	public boolean timerRunning( UUID playerid ) {
		
		return ( playertimers.containsKey( playerid ) && playertimers.get( playerid ).isStarted() ) ? true : false;
	}
	
	/*
	 * Get the current timer time in milliseconds of a player timer
	 * 
	 * @param UUID of the player
	 * @return the time in milliseconds as a string if the player has a timer, or null
	 */
	public String GetTime( UUID playerid ) {
		
		if ( playertimers.containsKey( playerid )) {
			
			return playertimers.get( playerid ).toString();
		}
		
		return null;
	}
	
	/*
	 * Start a task to display the time of a player timer on the action bar
	 * and stop the timer and cancel the task if the timer has been stopped
	 * 
	 * @param UUID of the player
	 */
	private void updatePlayerTimer( UUID playerid ) {
		
		Player player = Bukkit.getPlayer( playerid );

		new BukkitRunnable() {
			
            @Override
            public void run() {

            	if ( playertimers.containsKey( playerid ) && !playertimers.get( player.getUniqueId() ).isStopped() ) {

                	if ( !playertasks.containsKey( playerid ) ) {
                		playertasks.put( playerid, this.getTaskId() );
                	}
                	
                	String time = Utils.formatTime( playertimers.get( playerid ).getTime(), "actionbar" );
                    time = ChatColor.GREEN + "Time: " + ChatColor.YELLOW + time;
                    TextComponent mtime = new TextComponent( time );
                    if ( player != null ) {
                        player.spigot().sendMessage( ChatMessageType.ACTION_BAR, mtime );
                    }

                    // cancel our task if our timer is stopped or removed
                    if ( playertimers.get( player.getUniqueId() ).isStopped() || !playertimers.containsKey( playerid ) ) {
                        this.cancel();
                        playertasks.remove( playerid );
                    }
                } else {
                	this.cancel();
                	playertasks.remove( playerid );
                }
            }
        }.runTaskTimer( OBAmazeingTimer.getInstance(), 1L, 1L );
    }
	
	/*
	 * Stop any timers running for a player and remove from our timer list
	 * 
	 * @param UUID of the player
	 */
	public void removePlayerTimer( UUID playerid ) {
		
		if ( !playertimers.containsKey( playerid ) ) {
			return;
		}
		
		if ( playertimers.get( playerid ).isStarted() ) {
			
			playertimers.get( playerid ).stop();
			playertimers.get( playerid ).reset();
			playertimers.remove( playerid );
		}
	}

	/*
	 * Remove a player timer task if running
	 * 
	 * @param UUID of the player
	 */
	public void removePlayerTask( UUID playerid ) {
		
		if ( playertasks.containsKey( playerid ) ) {
			
			if ( Bukkit.getScheduler().isCurrentlyRunning( playertasks.get( playerid ) ) ) {
				Bukkit.getScheduler().cancelTask( playertasks.get( playerid ) );
			}

			playertasks.remove( playerid );
		}
	}

}

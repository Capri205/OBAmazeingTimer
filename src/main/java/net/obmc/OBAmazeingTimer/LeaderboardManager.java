package net.obmc.OBAmazeingTimer;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LeaderboardManager {

	static Logger log = Logger.getLogger("Minecraft");

	private Map<Long, UUID> leaderboard = new TreeMap<Long, UUID>();
	private String logmsgprefix = null;

	private String leaderboardfilename = "leaderboard.yml";
	private FileConfiguration leaderboardconfig = null;
	
	/*
	 * Manages the in-game leaderboard and the leaderboard configuration file
	 * 
	 * Note: The in-game leaderboard is a TreeMap of time and player uuid
	 * whilst the leaderboard file is a hash of player uuid and time. A TreeMap
	 * is used so entries are always in time order for easier rendering of the
	 * leaderboard, whilst the file has player id key to maintain uniqueness of key
	 */
	public LeaderboardManager() {

		logmsgprefix = OBAmazeingTimer.getLogMsgPrefix();

		if ( leaderboard == null ) {
			leaderboard = new HashMap<Long, UUID>();
		}
		
		File leaderboardfile = new File( OBAmazeingTimer.getInstance().getDataFolder(), leaderboardfilename );
		if ( !leaderboardfile.exists() ) {
			leaderboardfile.getParentFile().mkdirs();
			OBAmazeingTimer.getInstance().saveResource( leaderboardfilename, false );
		}
	}

	/*
	 * Return the current leaderboard map
	 * 
	 * @return the leaderboard treemap
	 */
	public Map<Long, UUID> getLeaderboard() {
		
		return leaderboard;
	}
	
	/*
	 * Build leaderboard from the leaderboard config file
	 * Note the swapping of time and uuid for the in-game TreeMap
	 * 
	 * @return true is the leaderboard was loaded and created, false if not
	 */
	public boolean loadLeaderboardFromConfig() {
		
		try {
			
			leaderboardconfig = YamlConfiguration.loadConfiguration( new File( OBAmazeingTimer.getInstance().getDataFolder(), leaderboardfilename ) );
			if ( leaderboardconfig.contains( "timings" ) ) {
				if ( leaderboardconfig.getConfigurationSection( "timings" ).getKeys( false ).size() > 0 ) {
					leaderboardconfig.getConfigurationSection( "timings" ).getKeys( false ).forEach(
						key -> leaderboard.put( leaderboardconfig.getConfigurationSection( "timings" ).getLong( key ), UUID.fromString( key ) )
					);
				}
			}
		} catch ( Exception e ) {
			
			log.log(Level.SEVERE, logmsgprefix + "Failed to load leaderboard from config file " + leaderboardfilename );
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/* 
	 * Save player time to the leaderboard file
	 * 
	 * @param time
	 * @param UUID of player
	 * @return true if the save went through without issue, false if an exception occurred
	 */
	public boolean saveToLeaderboardFile( Long time, UUID playerid ) {
		
		try {
			
			if ( !leaderboardconfig.contains( "timings" ) ) {
				leaderboardconfig.createSection( "timings" );
			}
			leaderboardconfig.getConfigurationSection( "timings" ).set( playerid.toString(), time );
			leaderboardconfig.save( new File( OBAmazeingTimer.getInstance().getDataFolder(), leaderboardfilename ) );
			
		} catch ( Exception e ) {
			log.log(Level.SEVERE, logmsgprefix + "Failed to save entry to the leaderboard to config file " + leaderboardfilename );
			return false;
		}
		
		return true;
	}
	
	/* 
	 * Remove a player time from the leaderboard config file
	 * 
	 * @param UUID of player
	 * @return true if we removed the entry, false if an exception was generated
	 */
	public boolean removeFromLeaderboardFile( UUID playerid ) {
		
		try {
			
			leaderboardconfig.getConfigurationSection( "timings" ).set( playerid.toString(), null );
			leaderboardconfig.save( new File( OBAmazeingTimer.getInstance().getDataFolder(), leaderboardfilename ) );
			
		} catch ( Exception e ) {
			log.log(Level.SEVERE, logmsgprefix + "Failed to remove entry from the leaderboard to config file " + leaderboardfilename );
			return false;
		}
		
		return true;
	}
	
	/*
	 * Add a players time to the leaderboard
	 * 
	 * @param time
	 * @param UUID of player
	 */
	public void addTime( Long time, UUID playerid ) {
		
		//remove existing time from leaderboard if there is one
		Long key = getPlayerExistingTime( playerid );
		if ( key != null ) {
			
			removeTime( key, playerid );
		}
		
		// put new time into leaderboard and save leaderboard config
		leaderboard.put( time,  playerid );
		saveToLeaderboardFile( time, playerid );
	}

	/*
	 * Remove a players time from the leaderboard and leaderboard file
	 * 
	 * @param time
	 * @param player UUID
	 */
	public void removeTime( Long time, UUID playerid ) {
		
		//TODO: additional checks required?
		
		Iterator<Long> lbit = leaderboard.keySet().iterator();
		while ( lbit.hasNext() ) {
			
			Long key = lbit.next();
			if ( key.equals( time ) && leaderboard.get( key ).toString().equals( playerid.toString() ) ) {
				
				lbit.remove();
			}
		}

		removeFromLeaderboardFile( playerid );
	}
	
	/*
	 * Retrieve a players time if one exists
	 * 
	 * @param UUID of player
	 * @return the key if player has a time, null if not
	 */
	public Long getPlayerExistingTime( UUID playerid ) {
		
		for ( Long key : leaderboard.keySet() ) {
			leaderboard.get( key );
			if ( leaderboard.get( key ).toString().equals( playerid.toString() ) ) {
				return key;
			}
		}

		return null;
	}

	/*
	 *  See where a time would rank - more recent time wins when times are the same
	 *  
	 *  @param time
	 *  @return the position in the leaderboard for the time
	 */
	public int getRankForTime( Long time ) {
		
		int rank = 1;
		for ( Long key : leaderboard.keySet() ) {
			if ( time <= key ) {
				return rank;
			}
			rank++;
		}
		
		return rank;
	}
}

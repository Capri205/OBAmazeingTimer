package net.obmc.OBAmazeingTimer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class OBAmazeingTimer extends JavaPlugin
{
	static Logger log = Logger.getLogger("Minecraft");
	
	public static OBAmazeingTimer instance;

	private EventListener listener;

	private MazeTrigger starttimertrigger;
	private MazeTrigger stoptimertrigger;

	private double mazehigh;
	private double mazelow;
	private Location mazecornerhigh;
	private Location mazecornerlow;
	
	private MazeTimer timers = new MazeTimer();
	private LeaderboardManager leaderboardmanager = null;
	private SignManager signmanager = null;

	private static String plugin = "OBAmazeingTimer";
	private static String pluginprefix = "[" + plugin + "]";
	private static String chatmsgprefix = ChatColor.AQUA + "" + ChatColor.BOLD + plugin + ChatColor.DARK_GRAY + ChatColor.BOLD + " » " + ChatColor.LIGHT_PURPLE + "";
	private static String logmsgprefix = pluginprefix + " » ";
	
	public OBAmazeingTimer() {
		instance = this;
	}

	/*
	 * Make our (public) main class methods and variables available to other classes
	 * 
	 * @return this instance 
	*/
	public static OBAmazeingTimer getInstance() {
    	return instance;
    }

	@Override
	public void onEnable() {
		// Initialize Stuff
		if ( !initializeStuff() ) {
			log.log(Level.INFO, logmsgprefix + "Failed to initialize plugin from config");
			
		}

		// Register stuff
		registerStuff();

		log.log(Level.INFO, getLogMsgPrefix() + " Plugin Version " + this.getDescription().getVersion() + " activated!");
	}

	// Plugin Stop
	public void onDisable() {
		
		log.log(Level.INFO, getLogMsgPrefix() + " Plugin deactivated!");
	}

	/*
	 * Load plugin config and set up various objects we need for the timer, like the
	 * triggers, leaderboard, and render the leaderboard in the world
	 * 
	 *  @return true if everything went ok, otherwise false
	 */
	public boolean initializeStuff() {

		this.saveDefaultConfig();
		Configuration config = this.getConfig();

		// setup entry location
		try {
			this.starttimertrigger = new MazeTrigger(
				new Location( Bukkit.getWorld( config.getConfigurationSection( "maze" ).getString( "world" ) ),
					config.getConfigurationSection( "starttimertrigger" ).getConfigurationSection("pad").getDouble( "x" ),
					config.getConfigurationSection( "starttimertrigger" ).getConfigurationSection("pad").getDouble( "y" ),
					config.getConfigurationSection( "starttimertrigger" ).getConfigurationSection("pad").getDouble( "z" )
				),
				Material.valueOf( config.getConfigurationSection( "starttimertrigger" ).getConfigurationSection( "pad" ).getString( "material" ) )
			);
		} catch( Exception e ) {
			log.log( Level.SEVERE, logmsgprefix + "Failed to setup maze start timer trigger" );
			e.printStackTrace();
			return false;
		}

		// setup exit location
		try {
			this.stoptimertrigger = new MazeTrigger(
				new Location( Bukkit.getWorld( config.getConfigurationSection( "maze" ).getString( "world" ) ),
					config.getConfigurationSection( "stoptimertrigger" ).getConfigurationSection( "pad" ).getDouble( "x" ),
					config.getConfigurationSection( "stoptimertrigger" ).getConfigurationSection( "pad" ).getDouble( "y" ),
					config.getConfigurationSection( "stoptimertrigger" ).getConfigurationSection( "pad" ).getDouble( "z" )
				),
				Material.valueOf( config.getConfigurationSection( "stoptimertrigger" ).getConfigurationSection("pad").getString( "material" ) )
			);
		} catch( Exception e ) {
			log.log( Level.SEVERE, logmsgprefix + "Failed to setup maze stop timer trigger" );
			e.printStackTrace();
			return false;
		}

		// read maze properties
		try {
			this.mazehigh = config.getConfigurationSection( "maze" ).getDouble( "high" );
			this.mazelow = config.getConfigurationSection( "maze" ).getDouble( "low" );
			this.mazecornerhigh = new Location(
				Bukkit.getWorld( config.getConfigurationSection( "maze" ).getString( "world" ) ),
				config.getConfigurationSection( "maze" ).getConfigurationSection( "sidehigh" ).getDouble( "x" ),
				this.mazehigh,
				config.getConfigurationSection( "maze" ).getConfigurationSection( "sidehigh" ).getDouble( "z" )
			);
			this.mazecornerlow = new Location(
				Bukkit.getWorld( config.getConfigurationSection( "maze" ).getString( "world" ) ),
				config.getConfigurationSection( "maze" ).getConfigurationSection( "sidelow" ).getDouble( "x" ),
				this.mazehigh,
				config.getConfigurationSection( "maze" ).getConfigurationSection( "sidelow" ).getDouble( "z" )
			);
			
		} catch( Exception e ) {
			log.log( Level.SEVERE, logmsgprefix + "Failed to read maze properties" );
			e.printStackTrace();
			return false;			
		}
		
		// read current leaderboard config, populate signs and render leaderboard
		leaderboardmanager = new LeaderboardManager();
		leaderboardmanager.loadLeaderboardFromConfig();
		signmanager = new SignManager();
		signmanager.loadSignsFromConfig();
		signmanager.populateSigns( leaderboardmanager );
		
		return true;
	}

	/*
	 * Register event listeners
	 */
	public void registerStuff() {
		
        this.listener = new EventListener();
        this.getServer().getPluginManager().registerEvents( (Listener)this.listener, (Plugin)this );
	}
	
    /*
     * Return the Y coordinate which represents the top of the maze
     * Used for bounds checking to is player exited the maze somehow
     * 
     * @return maze high value
     */
	public double getMazeHigh() {
		
		return this.mazehigh;
	}
	
    /*
     * Return the Y coordinate which represents the bottom of the maze
     * Used for bounds checking to is player exited the maze somehow
     * 
     * @return maze low value
     */
	public double getMazeLow() {
		
		return this.mazelow;
	}
	
    /*
     * Return the location which represents the high corner of the maze
     * Used for bounds checking to is player exited the maze somehow
     * 
     * @return maze high corner
     */
	public Location getMazeSideHigh() {
		
		return this.mazecornerhigh;
	}

    /*
     * Return the location which represents the low corner of the maze
     * Used for bounds checking to is player exited the maze somehow
     * 
     * @return maze low corner
     */
	public Location getMazeSideLow() {
		
		return this.mazecornerlow;
	}
	
	/*
	 * Return a maze timer manager
	 * 
	 * @return {@link MazeTimer}
	 */
	public MazeTimer getMazeTimer() {
		
		return this.timers;
	}

	/*
	 * Return the maze start timer trigger
	 * 
	 * @return {@link MazeTrigger}
	 */
	public MazeTrigger getStartTimerTrigger() {
		
		return starttimertrigger;
	}

	/*
	 * Return the maze stop timer trigger
	 * 
	 * @return {@link MazeTrigger}
	 */
	public MazeTrigger getStopTimerTrigger() {
		return stoptimertrigger;
	}

	/*
	 * Return the leaderboard manager
	 * 
	 * @return {@link LeaderboardManager}
	 */
	public LeaderboardManager getLeaderboardManager() {
		return this.leaderboardmanager;
	}
	
	/*
	 * Return the sign manager
	 * 
	 * @return {@link SignManager}
	 */
	public SignManager getSignManager() {
		return this.signmanager;
	}

	/*
	 * Return the name of the plugin
	 * 
	 * @return plugin name as a string
	 */
	public static String getPluginName() {
		return plugin;
	}

	/*
	 * Return the prefix used by the plugin for player and log messaging
	 * 
	 * @return prefix as a string
	 */
	public static String getPluginPrefix() {
		return pluginprefix;
	}

	/*
	 * Return the prefix used by the plugin for player messaging
	 * 
	 * @return prefix as a string
	 */
	public static String getChatMsgPrefix() {
		return chatmsgprefix;
	}

	/*
	 * Return the prefix used by the plugin for logging
	 * 
	 * @return prefix as a string
	 */
	public static String getLogMsgPrefix() {
		return logmsgprefix;
	}
}

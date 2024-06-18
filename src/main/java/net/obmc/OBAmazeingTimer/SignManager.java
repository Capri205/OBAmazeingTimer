package net.obmc.OBAmazeingTimer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

public class SignManager {

	static Logger log = Logger.getLogger("Minecraft");

	private Map<String, Location> signs = new HashMap<String, Location>();

	private String logmsgprefix = null;

	private String signsfilename = "signs.yml";
	private boolean redrawtitlesigns = true;
	private String worldname = "world";
	private int signrows = 3;
	private BlockFace signdirection = BlockFace.EAST;
	private Material titlesignmaterial = null;
	private Material titlebackmaterial = null;
	private ChatColor titletextcolor = null;
	private Material leaderboardsignmaterial = null;
	private Material leaderboardbackmaterial = null;
	private ChatColor leaderboardtextcolor = null;
	
	/*
	 * Draws the in-game leaderboard using signs and backing blocks
	 * 
	 * Sign material, backing block material and sign text color and
	 * the number of rows of signs (size of the leaderboard) as well
	 * as the direction the leaderboard faces  are set in the signs
	 * configuration file.
	 */
	public SignManager() {

		if ( signs == null ) {
			signs = new HashMap<String, Location>();
		}
		
		OBAmazeingTimer.getChatMsgPrefix();
		logmsgprefix = OBAmazeingTimer.getLogMsgPrefix();
		
		File signsfile = new File( OBAmazeingTimer.getInstance().getDataFolder(), signsfilename );
		if ( !signsfile.exists() ) {
			signsfile.getParentFile().mkdirs();
			OBAmazeingTimer.getInstance().saveResource( signsfilename, false );
		}
	}
	
	/*
	 * Return the map of sign locations
	 * 
	 * @return hash of sign type and its location
	 */
	public Map<String, Location> getSigns() {
		return signs;
	}
	
	/*
	 * Load up our title and leaderboard sign hashes with the data from the config file
	 *  
	 * @return true if no issued encountered, false if not
	 */
	public boolean loadSignsFromConfig() {

		FileConfiguration fileload = YamlConfiguration.loadConfiguration( new File( OBAmazeingTimer.getInstance().getDataFolder(), signsfilename ) );
		if ( fileload == null ) {
			log.log( Level.SEVERE, logmsgprefix + "No signs file found in the plugin folder");
			return false;
		}
		
		if ( !fileload.contains( "signs" ) || !fileload.contains( "signrows" ) ) {
			log.log( Level.INFO, logmsgprefix + "Signs file is missing a signs section or the number of signs value");
			return false;
		}

		double[] coords = { 0, 0, 0 };
		
		try {
			
			worldname = OBAmazeingTimer.getInstance().getConfig().getConfigurationSection( "maze" ).getString( "world" );
			redrawtitlesigns = fileload.getBoolean( "redrawtitlesigns" );
			signrows = fileload.getInt( "signrows" );

			// load title sign config from signs file
			ConfigurationSection signsconfig = fileload.getConfigurationSection( "signs" );
			signdirection = BlockFace.valueOf( signsconfig.getString( "facing" ) );
			ConfigurationSection titleconfig = signsconfig.getConfigurationSection( "title" ); 
			titlesignmaterial = Material.valueOf( titleconfig.getString( "signmaterial" ) );
			titlebackmaterial = Material.valueOf( titleconfig.getString( "backingmaterial" ) );
			titletextcolor = ChatColor.of( titleconfig.getString( "textcolor" ) );
			coords[0] = titleconfig.getConfigurationSection( "startlocation" ).getDouble( "x" );
			coords[1] = titleconfig.getConfigurationSection( "startlocation" ).getDouble( "y" );
			coords[2] = titleconfig.getConfigurationSection( "startlocation" ).getDouble( "z" );
				
			// we are fixed with 3 sign wide (rank, name and time), but it can be any number of sign rows
			for ( int signnum = 0; signnum < 3; signnum++ ) {
				
				signs.put( "title" + signnum, new Location ( Bukkit.getWorld( worldname ), coords[0], coords[1], coords[2] ) );
				
				// repeat the signs in the correct direction based on which way the signs are facing
				coords = nextLocationByDirection( signdirection, coords );
			}
			
			// load leaderboard sign config
			ConfigurationSection leaderboardconfig = signsconfig.getConfigurationSection( "leaderboard" );
			leaderboardsignmaterial = Material.valueOf( leaderboardconfig.getString( "signmaterial" ) );
			leaderboardbackmaterial = Material.valueOf( leaderboardconfig.getString( "backingmaterial" ) );
			leaderboardtextcolor = ChatColor.of( leaderboardconfig.getString( "textcolor" ) );
			coords[0] = leaderboardconfig.getConfigurationSection( "startlocation" ).getDouble( "x" );
			coords[1] = leaderboardconfig.getConfigurationSection( "startlocation" ).getDouble( "y" );
			coords[2] = leaderboardconfig.getConfigurationSection( "startlocation" ).getDouble( "z" );
			
			// generate a rank, player and time sign for however many rows on the leaderboard
			// we are fixed 3 signs wide - rank, name and time
			// there are 4 ranks per sign, so 3 signs would give 12 ranks
			for ( int signrow = 0; signrow < signrows ; signrow++ ) {
				
					signs.put( "ranksign" + signrow, new Location( Bukkit.getWorld( worldname ), coords[0], coords[1], coords[2] ) );
					coords = nextLocationByDirection( signdirection, coords );

					signs.put( "namesign" + signrow, new Location( Bukkit.getWorld( worldname ), coords[0], coords[1], coords[2] ) );

					coords = nextLocationByDirection( signdirection, coords );
					signs.put( "timesign" + signrow, new Location( Bukkit.getWorld( worldname ), coords[0], coords[1], coords[2] ) );

					coords[0] = leaderboardconfig.getConfigurationSection( "startlocation" ).getDouble( "x" );
					coords[1]--;
					coords[2] = leaderboardconfig.getConfigurationSection( "startlocation" ).getDouble( "z" );
			}
		} catch ( Exception e ) {
			
			log.log(Level.SEVERE, logmsgprefix + "Failed to load leaderboard sign data from sign file " + signsfilename );
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/*
	 * Render the leaderboard in the world. We create the backing block and the signs
	 * on them to represent the leaderboard title and data from the leaderboard manager
	 * 
	 * @param {@link LeaderboardManager}
	 */
	public void populateSigns( LeaderboardManager leaderboardmanager ) {
	
		Block signblock = null;
		
		// redraw the title signs if required
		if ( redrawtitlesigns ) {
			for ( int signnum = 0; signnum < 3; signnum++ ) {
				
				signblock = Bukkit.getWorld( worldname ).getBlockAt( signs.get( "title" + signnum ) );
				setSignBlock( signblock, titlesignmaterial, titlebackmaterial );
				
				Sign titlesign = (Sign) Bukkit.getWorld( worldname ).getBlockAt( signs.get( "title" + signnum ) ).getState();
				for ( int line = 0; line < 4; line++ ) {
					if ( signnum == 1 && line == 1 ) {
						titlesign.getSide(Side.FRONT).setLine( line, ChatColor.AQUA + "A-MAZE-ING" );
					} else if ( signnum == 1 && line == 2 ) {
						titlesign.getSide(Side.FRONT).setLine( line, ChatColor.GREEN + "LEADERBOARD" );
					} else {
						titlesign.getSide(Side.FRONT).setLine( line, titletextcolor + "***************" );
					}
				}
				titlesign.update();
			}
		}
		
		ArrayList<Long> playertimes = new ArrayList<Long>( leaderboardmanager.getLeaderboard().keySet() );
		ArrayList<UUID> playerids = new ArrayList<UUID>( leaderboardmanager.getLeaderboard().values() );
		int numleaders = playerids.size();

		// populate however many rows of signs with 4 lines each with as much data as we have and the remaining with a message
		int prevsignnum = -1;
		Sign ranksign = null;
		Sign namesign = null;
		Sign timesign = null;
		for ( int ranknum = 0; ranknum < ( signrows * 4 ); ranknum++ ) {

			int signnum = (int)( ranknum / 4 ) + 1;
			int signlinenum = ranknum - ((int)( ranknum / 4 ) * 4 );
			
			// detect when we've moved from one row of signs to the next
			// a row of signs is a rank, name and time sign. Four ranks/lines per sign set
			if ( signnum != prevsignnum ) {
				
				signblock = Bukkit.getWorld( worldname ).getBlockAt( signs.get( "ranksign" + ( signnum - 1 ) ) );
				setSignBlock( signblock, leaderboardsignmaterial, leaderboardbackmaterial );
				ranksign = (Sign) signblock.getState();

				signblock = Bukkit.getWorld( worldname ).getBlockAt( signs.get( "namesign" + (signnum - 1 ) ) );
				setSignBlock( signblock, leaderboardsignmaterial, leaderboardbackmaterial );
				namesign = (Sign) signblock.getState();

				signblock = Bukkit.getWorld( worldname ).getBlockAt( signs.get( "timesign" + ( signnum - 1 ) ) );
				setSignBlock( signblock, leaderboardsignmaterial, leaderboardbackmaterial );
				timesign = (Sign) signblock.getState();

				prevsignnum = signnum;
			}

			// put text on our rank, name and time signs for this row on the leaderboard
			if ( ranknum < numleaders ) {
				
				ranksign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + "" + ( ranknum  + 1 ) + "." );
				namesign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + Bukkit.getOfflinePlayer( playerids.get( ranknum ) ).getName() );
				timesign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + Utils.formatTime( playertimes.get( ranknum ), "leaderboard" ) );
				
			} else {
				ranksign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + "" + ( ranknum + 1 ) + ".");
				namesign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + "Your name here!" );
				timesign.getSide(Side.FRONT).setLine( signlinenum, leaderboardtextcolor + "Your time here!" );
			}

			// send players the sign update event so they see the signs updated in-game
			for (Player player : Bukkit.getOnlinePlayers()) {
				// reinstate once bug fixed in 1.20.1
				//player.sendSignChange( signs.get( "ranksign" + ( signnum - 1 ) ), ranksign.getSide(Side.FRONT).getLines() );
				//player.sendSignChange( signs.get( "namesign" + ( signnum - 1 ) ), namesign.getSide(Side.FRONT).getLines() );
				//player.sendSignChange( signs.get( "timesign" + ( signnum - 1 ) ), timesign.getSide(Side.FRONT).getLines() );
			}
			ranksign.update( true );
			namesign.update( true );
			timesign.update( true );
		}
	}

	/*
	 * Sets the backing block and sign at the sign block location
	 * 
	 * @param {@link Block} the block for the sign
	 * @param {@link Material} the sign material
	 * @param {@link Material} the backing block material
	 */
	private void setSignBlock( Block signblock, Material sign, Material backing ) {
		
		Block backblock = signblock.getRelative( signdirection.getOppositeFace() );
		if ( !backblock.equals( backing ) ) {
			
			backblock.setType( backing );
		}
		
		signblock.setType( sign );
		Utils.setSignFacing( signblock, signdirection );
	}

	/*
	 * Get next block location based on direction
	 * 
	 * @param {@link BlockFace} direction
	 * @param x, -x, z and -z array
	 * @return adjusted coordinates indicating the next block location
	 */
	private double[] nextLocationByDirection( BlockFace direction, double[] coords ) {
		if ( direction.equals( BlockFace.EAST ) ) {
			coords[2]--;
		} else if ( direction.equals( BlockFace.SOUTH ) ) {
			coords[0]++;
		} else if ( direction.equals( BlockFace.WEST ) ) {
			coords[2]++;
		} else {
			coords[0]--;
		}
		return coords;
	}
}

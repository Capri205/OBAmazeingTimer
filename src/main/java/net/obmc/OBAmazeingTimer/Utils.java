package net.obmc.OBAmazeingTimer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;

public class Utils {

	static Logger log = Logger.getLogger("Minecraft");

	/*
	 * Format time into a message for the action bar, final time message or leaderboard
	 * 
	 * @param time in milliseconds
	 * @param type of formatting - final time, action bar or leaderboard
	 * @return formatted time string
	 */
	public static String formatTime( long time, String type ) {
		
		//long milliseconds = ( time % 1000 );
        //int two_digit_ms = (int) (milliseconds/10);
        int totalseconds = (int) ( time /1000.0 );
        int hours = (int)( totalseconds / 3600 );
        int minutes = (int)( ( totalseconds % 3600 ) / 60 );
        int seconds = totalseconds % 60;

        String hrsstr = ""; String minstr = ""; String secstr = "";
        
        if ( type.equals( "final" ) ) {
        	
        	// final time formatting - when player exits the maze
	    	hrsstr = "%1d hour" + ( hours != 1 ? "s" : "" );
	    	minstr = "%1d minute" + ( minutes != 1 ? "s" : "" );
	    	secstr = "%1d second" + ( seconds != 1 ? "s" : "" );
        	if (hours > 0) {
   	    		return String.format( hrsstr + " " + minstr + " " + secstr, hours, minutes, seconds );
        	} else {
            	if ( minutes > 0 ) {
            		return String.format( minstr + " " + secstr, minutes, seconds );
            	} else {
            		return String.format( secstr, seconds );
            	}
        	}
        } else if ( type.equals( "actionbar" ) ) {
        	
        	// action bar formatting - condensed to just digits
        	if ( hours > 0 ) {
        		return String.format("%01d:%02d:%02d", hours, minutes, seconds );        		
        	} else {
        		return String.format("%01d:%02d", minutes, seconds );
        	}
        } else {
        	
        	// leaderboard formatting - shortened wording
	    	hrsstr = "%1d hr" + ( hours != 1 ? "s" : "" );
	    	minstr = "%1d min" + ( minutes != 1 ? "s" : "" );
	    	secstr = "%1d sec" + ( seconds != 1 ? "s" : "" );
        	if ( hours > 0 ) {
   	    		return String.format( hrsstr + " " + minstr + " " + secstr, hours, minutes, seconds);
        	} else {
            	if ( minutes > 0 ) {
            		return String.format( minstr + " " + secstr, minutes, seconds);
            	} else {
            		return String.format( secstr, seconds);
            	}
        	}
        }
	}
	
	/*
	 * Set the direction of a sign at a block location
	 * 
	 * @param {@link Block} block
	 * @param {@link BlockFace} direction
	 */
	public static void setSignFacing( Block block, BlockFace face ){
		
		if ( block.getState() instanceof Sign ) {
			
			Sign sign = (Sign) block.getState();
			if ( sign.getBlockData() instanceof WallSign ) {
				
				WallSign signData = (WallSign) sign.getBlockData();
				signData.setFacing( face );
				sign.setBlockData( signData );
				block.setBlockData( sign.getBlockData() );
				sign.update();
			}
		}
	}
	
    /*
     * Get player name from Mojang if they've expired from the server user cache
     * 
     * @param player uuid
     * @return player name
     */
    public static String fetchPlayerName(UUID uuid) {
        try {
            String apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-", "");
            URI uri = URI.create(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            
            int status = connection.getResponseCode();
            if (status != 200) {
                return "Unknown";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.toString());
                return root.get("name").asText();
            }

        } catch (Exception e) {
            log.log(Level.INFO, "Unable to get player from Mojan API. Using 'Unknown'");
            return "Unknown";
        }
    }
}
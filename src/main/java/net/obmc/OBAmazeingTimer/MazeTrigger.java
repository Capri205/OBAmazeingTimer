package net.obmc.OBAmazeingTimer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class MazeTrigger {

	private Location padlocation;
	private Material padmaterial;
	
	/**
     * Construct a new {@link MazeLocation}. Represents a trigger for entering or
     * exiting the maze, such as pressure plates
     *
     * @param padloc the location of the pad
     * @param padmat the material of the pad
     */
	public MazeTrigger( @NotNull Location padloc, @NotNull Material padmat ) {
		this.padlocation = padloc;
		this.padmaterial = padmat;
	}


	/*
	 * Returns the location of a trigger
	 * 
	 * @return {@link Location}
	 */
	public Location getPadLocation() {
		return this.padlocation;
	}

	/*
	 * Returns the X coordinate of a trigger location
	 * 
	 * @return X coordinate as integer
	 */
	public int getPadLocationX() {
		return (int) this.padlocation.getX();
	}

	/*
	 * Returns the Y coordinate of a trigger location
	 * 
	 * @return Y coordinate as integer
	 */
	public int getPadLocationY() {
		
		return (int) this.padlocation.getY();
	}

	/*
	 * Returns the Z coordinate of a trigger location
	 * 
	 * @return Z coordinate as integer
	 */
	public int getPadLocationZ() {

		return (int) this.padlocation.getZ();
	}
	
	/*
	 * Sets the location of a trigger
	 * 
	 * @param {@link Location} of the trigger
	 */
	public void setPadLocation( Location padloc ) {

		this.padlocation = padloc;
	}

	/*
	 * Returns the material type of a trigger
	 * 
	 * @return {@link Material} type
	 */
	public Material getPadMaterial() {

		return this.padmaterial;
	}

	/*
	 * Sets the material type of a trigger
	 * 
	 * @param {@link Material} type
	 */
	public void setPadMaterial( Material padmat ) {

		this.padmaterial = padmat;
	}
	
}

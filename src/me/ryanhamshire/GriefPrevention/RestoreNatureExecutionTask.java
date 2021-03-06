/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package me.ryanhamshire.GriefPrevention;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

//this main thread task takes the output from the RestoreNatureProcessingTask\
//and updates the world accordingly
class RestoreNatureExecutionTask implements Runnable 
{
	//results from processing thread
	//will be applied to the world
	private BlockSnapshot[][][] snapshots;
	
	//boundaries for changes
	private int miny;
	private Location lesserCorner;
	private Location greaterCorner;
	
	//player who should be notified about the result (will see a visualization when the restoration is complete)
	private Player player;

	public RestoreNatureExecutionTask(BlockSnapshot[][][] snapshots, int miny, Location lesserCorner, Location greaterCorner, Player player)
	{
		this.snapshots = snapshots;
		this.miny = miny;
		this.lesserCorner = lesserCorner;
		this.greaterCorner = greaterCorner;
		this.player = player;
	}
	
	@Override
	public void run()
	{
		//apply changes to the world, but ONLY to unclaimed blocks
		//note that the edge of the results is not applied (the 1-block-wide band around the outside of the chunk)
		//those data were sent to the processing thread for referernce purposes, but aren't part of the area selected for restoration
		Claim cachedClaim = null;
		for(int x = 1; x < this.snapshots.length - 1; x++)
		{
			for(int z = 1; z < this.snapshots[0][0].length; z++)
			{
				for(int y = this.miny; y < this.snapshots[0].length; y++)
				{
					BlockSnapshot blockUpdate = this.snapshots[x][y][z];
					Block currentBlock = blockUpdate.location.getBlock();
					if(blockUpdate.typeId != currentBlock.getTypeId() || blockUpdate.data != currentBlock.getData())
					{
						Claim claim = GriefPrevention.instance.dataStore.getClaimAt(blockUpdate.location, false, cachedClaim);
						if(claim != null)
						{
							cachedClaim = claim;
							break;
						}
						
						currentBlock.setTypeId(blockUpdate.typeId);
						currentBlock.setData(blockUpdate.data);
					}
				}
			}
		}
		
		//clean up any entities in the chunk
		Chunk chunk = this.lesserCorner.getChunk();
		Entity [] entities = chunk.getEntities();
		for(int i = 0; i < entities.length; i++)
		{
			Entity entity = entities[i];
			if(!(entity instanceof Player)) entity.remove();			
		}
		
		//show visualization to player
		Claim claim = new Claim(lesserCorner, greaterCorner, "", new String[] {}, new String[] {}, new String[] {}, new String[] {}, null);
		Visualization visualization = Visualization.FromClaim(claim, player.getLocation().getBlockY(), VisualizationType.RestoreNature);
		Visualization.Apply(player, visualization);
	}	
}

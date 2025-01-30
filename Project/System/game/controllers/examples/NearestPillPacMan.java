package game.controllers.examples;

import java.util.ArrayList;
import game.controllers.PacManController;
import game.core.G;
import game.core.Game;

public class NearestPillPacMan implements PacManController
{	
	public int getAction(Game game,long timeDue)
	{	
		int[] pills=game.getPillIndices();
		int[] powerPills=game.getPowerPillIndices();		
		int current=game.getCurPacManLoc();
		
		ArrayList<Integer> targets=new ArrayList<Integer>();
		
		for(int i=0;i<pills.length;i++)			//check which pills are available			
			if(game.checkPill(i))
				targets.add(pills[i]);
		
		for(int i=0;i<powerPills.length;i++)	//check with power pills are available
			if(game.checkPowerPill(i))
				targets.add(powerPills[i]);
		
		int[] targetsArray=new int[targets.size()];		//convert from ArrayList to array
		
		for(int i=0;i<targetsArray.length;i++)
			targetsArray[i]=targets.get(i);
		
		//return the next direction once the closest target has been identified
		return game.getNextPacManDir(game.getTarget(current,targetsArray,true,G.DM.PATH),true,Game.DM.PATH);	
	}
}
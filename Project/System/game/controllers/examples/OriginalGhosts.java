package game.controllers.examples;

import game.controllers.GhostController;
import game.core.Game;
import game.core.Game.DM;

/*
 * Original Pac-man Ghost AI. There are some differences just because of the 
 * limitations of the system.
 * @author John Wileczek
 */
public class OriginalGhosts implements GhostController
{
	//Need to keep track of a previous and current game states for
	//timer and such
	Game previousGameState;
	Game currentGameState;
	
	
	//Why you ask? Because Enums in java are stupid.
	private final int Blinky = 0;
	private final int Pinky = 1;
	private final int Inky = 3;
	private final int Clyde = 2;
	
	
	enum StateType
	{
		Scatter,
		Chase,
		Frightened,
		Lair,
		NoChange
	}
	
	//Interface State Class
	//All states are derived from this and need to
	//overload these functions
	private interface IState
	{
		public void UpdateStateTimer(long currentTimeMS);
		
		public abstract StateType getStateID();
		public abstract StateType getNextState(int ghostID);
		
		public abstract int getDirectionToMove(int ghostID);
		
		public abstract void Reset();
	}
	
	//Lair State
	//Ghost stays in this state when inside the lair
	//Changes to scatter wont lairtime is up
	private class LairState implements IState
	{
		@Override 
		public void UpdateStateTimer(long currentTimeMS)
		{
			
		}
		@Override
		public StateType getNextState(int ghostID)
		{
			//If we are outside of the lair then we want to change to scatter state
			if(currentGameState.getLairTime(ghostID) <= 0)
			{
				return StateType.Scatter;
			}
			
			return StateType.NoChange;
		}
		
		@Override
		public int getDirectionToMove(int ghostID)
		{
			return -1;
		}
		
		@Override
		public void Reset()
		{
			
		}
		
		@Override
		public StateType getStateID()
		{
			return StateType.Lair;
		}
	}
	
	//Frightened State
	//Once Ms Pacman eats a pill the ghosts will go to this state
	//In this state they will go to their respected powerpill corners
	private class FrightenedState implements IState
	{
		@Override 
		public void UpdateStateTimer(long currentTimeMS)
		{
			
		}
		
		@Override
		public StateType getNextState(int ghostID)
		{
			//If we are eaten in frightened state then we want to go to lair state
			if(currentGameState.getLairTime(ghostID) > 0)
			{
				return StateType.Lair;
			}
			
			//If the timer runs out and we are still in frightened state
			//then we want to change back to the previous state
			if(currentGameState.isEdible(ghostID) == false)
			{
				return previousGhostStates[ghostID].getStateID();
			}
			
			return StateType.NoChange;
		}
		
		@Override
		public StateType getStateID()
		{
			return StateType.Frightened;
		}
		
		@Override
		public int getDirectionToMove(int ghostID)
		{
			
			int[] powerPillIndices = currentGameState.getPowerPillIndices();
			if(ghostID == Blinky)
			{
				//Top right
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[1],true,DM.PATH);
			}
			else if(ghostID == Pinky)
			{
				//Top Left
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[0],true,DM.PATH);
			}
			else if(ghostID == Inky)
			{
				//Bottom Right
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[3],true,DM.PATH);
			}
			else if(ghostID == Clyde)
			{
				//Bottom Left
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[2],true,DM.PATH);
			}
			
			return -1;
		}
		
		@Override
		public void Reset()
		{
			
		}
	}
	
	//Scatter State
	//First State after Lair State. Each ghost will stay in scatter for either 7 seconds or 5 seconds
	//before changing to chase
	private class ScatterState implements IState
	{
		private long previousTimeMS = 0;
		private long fDT = 0;
		private long stateTimer = 0;
		private int toChaseSwitches = 0;
		
		@Override
		public void UpdateStateTimer(long currentTimeMS)
		{
			fDT = 40;
			
			stateTimer += fDT;
			
			previousTimeMS = currentTimeMS;
		}
		
		@Override
		public StateType getStateID()
		{
			return StateType.Scatter;
		}
		
		@Override
		public void Reset()
		{
			stateTimer = 0;
			previousTimeMS = 0;
			fDT = 0;
		}
		
		@Override
		public int getDirectionToMove(int ghostID)
		{
			
			int[] powerPillIndices = currentGameState.getPowerPillIndices();
			
			switch(ghostID)
			{
			case Blinky:
				//Blinky's scatter behavior is special based on the number of times he has
				//switched from scatter to chase. On the third or later scatter he scattered toward pacman's
				//location essentially keeping him permanently in chase.
				if(toChaseSwitches >= 2)
					return currentGameState.getNextGhostDir(ghostID,currentGameState.getCurPacManLoc(),true,DM.PATH);
				
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[1],true,DM.PATH);
			case Pinky:
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[0],true,DM.PATH);
			case Inky:
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[3],true,DM.PATH);
			case Clyde:
				return currentGameState.getNextGhostDir(ghostID,powerPillIndices[2],true,DM.PATH);
			}
			return -1;
		}
		
		@Override
		public StateType getNextState(int ghostID)
		{
			
			if(currentGameState.isEdible(ghostID))
			{
				return StateType.Frightened;
			}
			
			if(toChaseSwitches < 2)
			{
				if(stateTimer >= 4000)
				{
					toChaseSwitches++;
					return StateType.Chase;
				}
			}
			else if(toChaseSwitches >= 2)
			{
				if(stateTimer >= 2000)
				{
					toChaseSwitches++;
					return StateType.Chase;
				}
			}
			return StateType.NoChange;
		}
		
		
	}
	
	//Chase State
	//This class will use the ghosts individual personalities to chase ms pacman
	//Chases occur for 20 seconds at a time then eventually becomes permanent after a certain number of
	//Scatter/Chase State Switches
	private class ChaseState implements IState
	{
		private long previousTimeMS = 0;
		private long fDT = 0;
		private long stateTimer = 0;
		private int toScatterSwitches = 0;
		
		public void UpdateStateTimer(long currentTimeMS)
		{
			fDT = 40;
    			
    		stateTimer += fDT;
    			
    		previousTimeMS = currentTimeMS;
		
		}
		
		@Override
		public StateType getStateID()
		{
			return StateType.Chase;
		}
		
		@Override
		public void Reset()
		{
			previousTimeMS = 0;
			fDT = 0;
			stateTimer = 0;
		}
		
		@Override
		public int getDirectionToMove(int ghostID)
		{
			switch(ghostID)
			{
			case Blinky:
				//Blinky always takes the shortest path directly to pacman
				return currentGameState.getNextGhostDir(ghostID,currentGameState.getCurPacManLoc(),true,DM.PATH);
			case Pinky:
			case Inky:
				//Pinky and Inky both look a certain number of nodes ahead of pacman to try to intercept
				//Pinky looks 4 nodes ahead, Inky looks 2 nodes ahead.
				//This portion is where the differences in behavior occurs because the node system
				//because if the node is invalid then it has no neighbours and messes up the look ahead
				//So sometimes it will just take the shortest path to pacman like blinky.
				int numNodesAhead = 4;
				if(ghostID == Inky)
				{
					numNodesAhead = 2;
				}
				int currPacManDirection = currentGameState.getCurPacManDir();
				int nodeTarget = currentGameState.getCurPacManLoc();
				
				
				for(int iAhead = 0; iAhead < numNodesAhead; iAhead++)
				{
					if(nodeTarget != -1)
					{
						nodeTarget = currentGameState.getNeighbour(nodeTarget,currPacManDirection);
					}
					
				}
				
				if(currPacManDirection == Game.UP)
				{
					for(int iLeft = 0; iLeft < numNodesAhead; iLeft++)
					{
						if(nodeTarget != -1)
							nodeTarget = currentGameState.getNeighbour(nodeTarget,Game.LEFT);
					}
				}
				if(nodeTarget != -1)
					return currentGameState.getNextGhostDir(ghostID,nodeTarget,true,DM.PATH);
				else
					return currentGameState.getNextGhostDir(ghostID,currentGameState.getCurPacManLoc(),true,DM.PATH);
				
			case Clyde:
				//If clyde is over a certain distance he goes straight for pacman
				//If he less than that distance from pacman he goes towards his scatter
				//location in the bottom left.
				int currPacManLoc = currentGameState.getCurPacManLoc();
				int[] powerPillIndices = currentGameState.getPowerPillIndices();
				int currScatterTarget = powerPillIndices[2];
				if(currentGameState.getPathDistance(currentGameState.getCurGhostLoc(ghostID),currPacManLoc) > 40)
				{
					return currentGameState.getNextGhostDir(ghostID,currPacManLoc,true,DM.PATH);
				}
				else
					return currentGameState.getNextGhostDir(ghostID,currScatterTarget,true,DM.PATH);
				
			}
			return currentGameState.getNextGhostDir(ghostID,currentGameState.getCurPacManLoc(),true,DM.PATH);
		}
		
		@Override
		public StateType getNextState(int ghostID)
		{
			if(currentGameState.isEdible(ghostID))
			{
				return StateType.Frightened;
			}
			
			if(toScatterSwitches < 3)
			{
				if(stateTimer >= 20000)
				{
					toScatterSwitches++;
					return StateType.Scatter;
				}
			}
			return StateType.NoChange;
		
		}
	}
	//States
	private ScatterState[] scatterStates = {new ScatterState(),new ScatterState(),new ScatterState(),new ScatterState()};
	private ChaseState[] chaseStates = {new ChaseState(),new ChaseState(),new ChaseState(),new ChaseState()};
	private FrightenedState[] frightenedStates = {new FrightenedState(),new FrightenedState(),new FrightenedState(),new FrightenedState()};
	private LairState[] lairStates = {new LairState(),new LairState(),new LairState(),new LairState()};
	
	
	private IState[] previousGhostStates = {null,null,null,null};
	private IState[] currentGhostStates = {lairStates[Blinky],lairStates[Pinky],lairStates[Inky],lairStates[Clyde]};
	//Place your game logic here to play the game as the ghosts
	public int[] getActions(Game game,long timeDue)
	{
		int[] ghostDirections = {-1,-1,-1,-1};
		
		currentGameState = game;
		if(previousGameState == null)
		{
			previousGameState = currentGameState;
		}
		for(int iGhost = 0; iGhost < 4; iGhost++)
		{
			ghostDirections[iGhost] = getNextAction(iGhost,timeDue);
			//ghostDirections[iGhost] = -1;
		}
		
		previousGameState = currentGameState;
		
		return ghostDirections;
	}
	
	//I have to reset some data or else it will be carried over
	//to the next test run and perform inconsistently.
	public void ResetControllerData()
	{
		previousGameState = null;
		currentGameState = null;
		for(int iState = 0; iState < 4; iState++)
		{
			scatterStates[iState] = new ScatterState();
			chaseStates[iState] = new ChaseState();
			frightenedStates[iState] = new FrightenedState();
			lairStates[iState] = new LairState();
			
			previousGhostStates[iState] = null;
			currentGhostStates[iState] = lairStates[iState];
		}
		
		
	}
	
	private int getNextAction(int ghostID,long currentTimeMS)
	{
		
			currentGhostStates[ghostID].UpdateStateTimer(currentTimeMS);

		
		StateType newState = currentGhostStates[ghostID].getNextState(ghostID);
		
		if(newState != StateType.NoChange)
		{
			switch(newState)
			{
			case Scatter:
				previousGhostStates[ghostID] = currentGhostStates[ghostID];
				currentGhostStates[ghostID] = scatterStates[ghostID];
				break;
			case Chase:
				previousGhostStates[ghostID] = currentGhostStates[ghostID];
				currentGhostStates[ghostID] = chaseStates[ghostID];
				break;
			case Frightened:
				previousGhostStates[ghostID] = currentGhostStates[ghostID];
				currentGhostStates[ghostID] = frightenedStates[ghostID];
				break;
			case Lair:
				previousGhostStates[ghostID] = currentGhostStates[ghostID];
				currentGhostStates[ghostID] = lairStates[ghostID];
				break;
			}
			
			//If we were not changing from frightened then we want to reset the state data
			if(previousGhostStates[ghostID].getStateID() != StateType.Frightened)
			{
				currentGhostStates[ghostID].Reset();
			}	
		}
		
		return currentGhostStates[ghostID].getDirectionToMove(ghostID);
	}
	
}
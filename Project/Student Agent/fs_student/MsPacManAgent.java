package fs_student;

import game.controllers.PacManController;
import game.core.G;
import game.core.Game;

public class MsPacManAgent implements PacManController//, Constants
{
	public int getAction(Game game, long time)
	{
		int[] directions=game.getPossiblePacManDirs(true);		//set flag as true to include reversals		
		return directions[G.rnd.nextInt(directions.length)];
	}
}
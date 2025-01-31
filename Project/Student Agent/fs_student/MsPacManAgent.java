package fs_student;

import game.controllers.PacManController;
import game.core.G;
import game.core.Game;

public class MsPacManAgent implements PacManController {
	// Constants for safe distances
	private static final int DANGER_DISTANCE = 10;  // Distance to consider a ghost dangerous
	private static final int VERY_DANGEROUS_DISTANCE = 5;  // Distance for immediate escape

	@Override
	public int getAction(Game game, long time) {
		// Get current position and possible directions
		int currentPos = game.getCurPacManLoc();
		int[] possibleDirs = game.getPossiblePacManDirs(true);  // Include reversals

		// If no valid moves, maintain current direction
		if (possibleDirs.length == 0) {
			return game.getCurPacManDir();
		}

		// Find the safest direction away from ghosts
		return findSafestDirection(game, currentPos, possibleDirs);
	}

	/**
	 * Find the safest direction that moves away from ghosts
	 */
	private int findSafestDirection(Game game, int currentPos, int[] possibleDirs) {
		int bestDirection = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

		// Evaluate each possible direction
		for (int dir : possibleDirs) {
			int nextNode = game.getNeighbour(currentPos, dir);
			if (nextNode != -1) {
				double score = evaluatePosition(game, nextNode);
				if (score > bestScore) {
					bestScore = score;
					bestDirection = dir;
				}
			}
		}

		// If no good direction found, choose a random one
		if (bestDirection == -1) {
			bestDirection = possibleDirs[G.rnd.nextInt(possibleDirs.length)];
		}

		return bestDirection;
	}

	/**
	 * Evaluate how safe a position is based on ghost distances and pills
	 */
	private double evaluatePosition(Game game, int position) {
		double score = 0.0;

		// Add score for distance from each ghost
		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i)) {  // Only consider non-edible ghosts as threats
				int ghostPos = game.getCurGhostLoc(i);
				int distance = game.getPathDistance(position, ghostPos);

				// Heavy penalty for very close ghosts
				if (distance < VERY_DANGEROUS_DISTANCE) {
					score -= 1000.0;
				}
				// Smaller penalty for ghosts within danger distance
				else if (distance < DANGER_DISTANCE) {
					score -= 500.0;
				}
				// Otherwise, add points for distance from ghosts
				else {
					score += distance * 2.0;
				}
			}
		}

		// Bonus for positions with pills (small incentive to collect them)
		if (game.checkPill(position)) {
			score += 20.0;
		}

		// Bonus for positions with power pills
		if (game.checkPowerPill(position)) {
			score += 50.0;
		}

		// Bonus for junctions (more escape options)
		if (game.isJunction(position)) {
			score += 30.0;
		}

		return score;
	}

	/**
	 * Checks if there are any dangerous ghosts nearby
	 */
	private boolean isDangerousGhostNearby(Game game, int position) {
		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i)) {
				int distance = game.getPathDistance(position, game.getCurGhostLoc(i));
				if (distance < DANGER_DISTANCE) {
					return true;
				}
			}
		}
		return false;
	}
}
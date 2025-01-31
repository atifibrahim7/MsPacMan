package fs_student;

import game.controllers.PacManController;
import game.core.G;
import game.core.Game;

public class MsPacManAgent implements PacManController {
	private static final int DANGER_DISTANCE = 10;
	private static final int VERY_DANGEROUS_DISTANCE = 5;
	private static final int CHASE_DISTANCE = 30;  // Distance to chase edible ghosts
	private int currentDirection = -1;

	@Override
	public int getAction(Game game, long time) {
		int currentPos = game.getCurPacManLoc();
		int[] possibleDirs = game.getPossiblePacManDirs(true);

		if (possibleDirs.length == 0) {
			return game.getCurPacManDir();
		}

		// Check if any ghosts are edible
		if (hasEdibleGhosts(game)) {
			// Chase mode - hunt ghosts
			return chaseGhosts(game, currentPos, possibleDirs);
		}
		// Normal mode - avoid dangerous ghosts
		else if (isDangerousGhostNearby(game, currentPos)) {
			currentDirection = -1;
			return findSafestDirection(game, currentPos, possibleDirs);
		} else {
			return handleSafeMovement(game, currentPos, possibleDirs);
		}
	}

	/**
	 * Check if there are any edible ghosts to chase
	 */
	private boolean hasEdibleGhosts(Game game) {
		for (int i = 0; i < 4; i++) {
			if (game.isEdible(i) && game.getEdibleTime(i) > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Chase edible ghosts - returns direction towards closest edible ghost
	 */
	private int chaseGhosts(Game game, int currentPos, int[] possibleDirs) {
		int bestDirection = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (int dir : possibleDirs) {
			int nextNode = game.getNeighbour(currentPos, dir);
			if (nextNode != -1) {
				double score = evaluateGhostChasePosition(game, nextNode);
				if (score > bestScore) {
					bestScore = score;
					bestDirection = dir;
				}
			}
		}

		return bestDirection != -1 ? bestDirection : possibleDirs[G.rnd.nextInt(possibleDirs.length)];
	}

	/**
	 * Evaluate position for ghost chasing
	 */
	private double evaluateGhostChasePosition(Game game, int position) {
		double score = 0.0;
		boolean hasEdibleGhostNearby = false;

		// Look for edible ghosts
		for (int i = 0; i < 4; i++) {
			if (game.isEdible(i)) {
				int ghostPos = game.getCurGhostLoc(i);
				int distance = game.getPathDistance(position, ghostPos);

				// Higher score for closer edible ghosts
				if (distance < CHASE_DISTANCE) {
					score += (CHASE_DISTANCE - distance) * 100.0;  // Prioritize ghost chasing
					hasEdibleGhostNearby = true;
				}
			}
		}

		// If no edible ghosts are nearby, consider pills and junctions
		if (!hasEdibleGhostNearby) {
			if (game.checkPill(position)) {
				score += 20.0;
			}
			if (game.checkPowerPill(position)) {
				score += 50.0;
			}
			if (game.isJunction(position)) {
				score += 30.0;
			}
		}

		return score;
	}

	private int handleSafeMovement(Game game, int currentPos, int[] possibleDirs) {
		if (currentDirection != -1 && canMoveInDirection(game, currentPos, currentDirection)) {
			return currentDirection;
		}
		currentDirection = possibleDirs[G.rnd.nextInt(possibleDirs.length)];
		return currentDirection;
	}

	private boolean canMoveInDirection(Game game, int currentPos, int direction) {
		return game.getNeighbour(currentPos, direction) != -1;
	}

	private int findSafestDirection(Game game, int currentPos, int[] possibleDirs) {
		int bestDirection = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

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

		return bestDirection != -1 ? bestDirection : possibleDirs[G.rnd.nextInt(possibleDirs.length)];
	}

	private double evaluatePosition(Game game, int position) {
		double score = 0.0;

		// Evaluate ghost threats
		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i)) {
				int ghostPos = game.getCurGhostLoc(i);
				int distance = game.getPathDistance(position, ghostPos);

				if (distance < VERY_DANGEROUS_DISTANCE) {
					score -= 1000.0;
				} else if (distance < DANGER_DISTANCE) {
					score -= 500.0;
				} else {
					score += distance * 2.0;
				}
			}
		}

		// Consider power pills more valuable when ghosts are nearby
		if (game.checkPowerPill(position) && isDangerousGhostNearby(game, position)) {
			score += 100.0;  // Increased bonus for power pills when ghosts are near
		} else if (game.checkPowerPill(position)) {
			score += 50.0;
		}

		// Regular bonuses
		if (game.checkPill(position)) {
			score += 20.0;
		}
		if (game.isJunction(position)) {
			score += 30.0;
		}

		return score;
	}

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
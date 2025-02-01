package fs_student;

import game.controllers.PacManController;
import game.core.G;
import game.core.Game;

public class MsPacManAgent implements PacManController {
	private static final int DANGER_DISTANCE = 10;
	private static final int VERY_DANGEROUS_DISTANCE = 5;
	private static final int CHASE_DISTANCE = 30;  // Distance to chase edible ghosts
	private int currentDirection = -1;
	private static final int LURE_DISTANCE = 15;    // Distance to start luring ghosts
	private static final int POWER_PILL_ACTIVATION = 4;
	private int ghostsEatenThisPowerPill = 0;
	private boolean isPowerPillActive = false;
	private static final int MAX_GHOSTS_TO_EAT = 3;
	@Override
	public int getAction(Game game, long time) {
		int currentPos = game.getCurPacManLoc();
		int[] possibleDirs = game.getPossiblePacManDirs(true);

		if (possibleDirs.length == 0) {
			return game.getCurPacManDir();
		}

		updatePowerPillStatus(game);

		if (ghostsEatenThisPowerPill >= MAX_GHOSTS_TO_EAT) {
			return handleNormalBehavior(game, currentPos, possibleDirs);
		}

		if (shouldLureGhosts(game, currentPos)) {
			return handlePowerPillLuring(game, currentPos, possibleDirs);
		} else if (hasEdibleGhosts(game)) {
			return chaseGhosts(game, currentPos, possibleDirs);
		} else if (isDangerousGhostNearby(game, currentPos)) {
			currentDirection = -1;
			return findSafestDirection(game, currentPos, possibleDirs);
		} else {
			return handleSafeMovement(game, currentPos, possibleDirs);
		}
	}


	private boolean hasEdibleGhosts(Game game) {
		for (int i = 0; i < 4; i++) {
			if (game.isEdible(i) && game.getEdibleTime(i) > 0) {
				return true;
			}
		}
		return false;
	}


	private int chaseGhosts(Game game, int currentPos, int[] possibleDirs) {
		if (ghostsEatenThisPowerPill >= MAX_GHOSTS_TO_EAT) {
			return handleSafeMovement(game, currentPos, possibleDirs);
		}

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
	private void updatePowerPillStatus(Game game) {
		boolean currentPowerPillActive = hasEdibleGhosts(game);

		if (currentPowerPillActive && !isPowerPillActive) {
			ghostsEatenThisPowerPill = 0;
		}

		if (currentPowerPillActive) {
			int currentGhostCount = countEdibleGhosts(game);
			if (currentGhostCount < countNonLairGhosts(game)) {
				ghostsEatenThisPowerPill++;
			}
		}

		isPowerPillActive = currentPowerPillActive;
	}
	/**
	 * Evaluate position for ghost chasing
	 */
	private double evaluateGhostChasePosition(Game game, int position) {
		double score = 0.0;
		boolean hasEdibleGhostNearby = false;

		if (ghostsEatenThisPowerPill < MAX_GHOSTS_TO_EAT) {
			for (int i = 0; i < 4; i++) {
				if (game.isEdible(i)) {
					int ghostPos = game.getCurGhostLoc(i);
					int distance = game.getPathDistance(position, ghostPos);

					if (distance < CHASE_DISTANCE) {
						score += (CHASE_DISTANCE - distance) * 100.0;
						hasEdibleGhostNearby = true;
					}
				}
			}
		}

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

	private int handleNormalBehavior(Game game, int currentPos, int[] possibleDirs) {
		if (isDangerousGhostNearby(game, currentPos)) {
			return findSafestDirection(game, currentPos, possibleDirs);
		} else {
			return handleSafeMovement(game, currentPos, possibleDirs);
		}
	}
	private int countNonLairGhosts(Game game) {
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (game.getLairTime(i) == 0) {
				count++;
			}
		}
		return count;
	}
	private int countEdibleGhosts(Game game) {
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (game.isEdible(i)) {
				count++;
			}
		}
		return count;
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

		if (game.checkPowerPill(position) && isDangerousGhostNearby(game, position)) {
			score += 100.0;
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

	private boolean shouldLureGhosts(Game game, int currentPos) {
		if (ghostsEatenThisPowerPill >= MAX_GHOSTS_TO_EAT) {
			return false;
		}

		int activeGhosts = countGhostsOutOfLair(game);
		if (activeGhosts < 2) {
			return false;
		}

		int nearestPowerPill = findNearestPowerPill(game, currentPos);
		if (nearestPowerPill != -1) {
			int distanceToPowerPill = game.getPathDistance(currentPos, nearestPowerPill);
			if (distanceToPowerPill <= LURE_DISTANCE) {
				int ghostCount = countNearbyActiveGhosts(game, nearestPowerPill, LURE_DISTANCE);
				return ghostCount >= 2;
			}
		}
		return false;
	}

	private int countGhostsOutOfLair(Game game) {
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (game.getLairTime(i) == 0) {  // Ghost is out of lair
				count++;
			}
		}
		return count;
	}

	private int countNearbyActiveGhosts(Game game, int position, int maxDistance) {
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i) && game.getLairTime(i) == 0) {  // Check if ghost is active and out of lair
				int distance = game.getPathDistance(position, game.getCurGhostLoc(i));
				if (distance <= maxDistance) {
					count++;
				}
			}
		}
		return count;
	}

	private int handlePowerPillLuring(Game game, int currentPos, int[] possibleDirs) {
		int nearestPowerPill = findNearestPowerPill(game, currentPos);
		if (nearestPowerPill == -1) return handleSafeMovement(game, currentPos, possibleDirs);

		int activeGhostCount = countNearbyActiveGhosts(game, nearestPowerPill, POWER_PILL_ACTIVATION);
		int distanceToPowerPill = game.getPathDistance(currentPos, nearestPowerPill);

		if (activeGhostCount >= 2 && distanceToPowerPill <= 2) {
			return game.getNextPacManDir(nearestPowerPill, true, Game.DM.PATH);
		}

		return maintainPositionNearPowerPill(game, currentPos, nearestPowerPill, possibleDirs);
	}
	/**
	 * Find the nearest power pill
	 */
	private int findNearestPowerPill(Game game, int currentPos) {
		int[] powerPills = game.getPowerPillIndices();
		int nearest = -1;
		int minDistance = Integer.MAX_VALUE;

		for (int pill : powerPills) {
			if (game.checkPowerPill(pill)) {
				int distance = game.getPathDistance(currentPos, pill);
				if (distance < minDistance) {
					minDistance = distance;
					nearest = pill;
				}
			}
		}
		return nearest;
	}


	/**
	 * Stay near power pill without eating it
	 */
	private int maintainPositionNearPowerPill(Game game, int currentPos, int powerPillPos, int[] possibleDirs) {
		int bestDir = -1;
		double bestScore = Double.NEGATIVE_INFINITY;

		int nearbyGhosts = countNearbyGhosts(game, powerPillPos, POWER_PILL_ACTIVATION);

		for (int dir : possibleDirs) {
			int nextNode = game.getNeighbour(currentPos, dir);
			if (nextNode != -1) {
				int distanceToPill = game.getPathDistance(nextNode, powerPillPos);

				if ((nearbyGhosts >= 2 && distanceToPill <= 2) ||
						(nearbyGhosts < 2 && distanceToPill < LURE_DISTANCE)) {

					double score = evaluateLuringPosition(game, nextNode, powerPillPos);

					if (nearbyGhosts >= 2) {
						score += (3 - distanceToPill) * 100.0;
					} else {
						score += (LURE_DISTANCE - Math.abs(LURE_DISTANCE/2 - distanceToPill)) * 10.0;
					}

					if (score > bestScore) {
						bestScore = score;
						bestDir = dir;
					}
				}
			}
		}

		return bestDir != -1 ? bestDir : findSafestDirection(game, currentPos, possibleDirs);
	}
	/**
	 * Evaluate position for luring ghosts
	 */
	private double evaluateLuringPosition(Game game, int position, int powerPillPos) {
		double score = 0.0;

		int distanceToPill = game.getPathDistance(position, powerPillPos);

		int closeGhosts = countNearbyGhosts(game, position, POWER_PILL_ACTIVATION);
		int luringGhosts = countNearbyGhosts(game, position, LURE_DISTANCE);

		score += closeGhosts * 100.0;
		score += luringGhosts * 50.0;

		if (game.isJunction(position)) {
			score += 30.0;
		}

		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i)) {
				int ghostDistance = game.getPathDistance(position, game.getCurGhostLoc(i));
				if (ghostDistance < VERY_DANGEROUS_DISTANCE) {
					score -= (distanceToPill <= 2) ? 250.0 : 500.0;
				}
			}
		}

		return score;
	}
	/**
	 * Count number of non-edible ghosts near a position
	 */
	private int countNearbyGhosts(Game game, int position, int maxDistance) {
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (!game.isEdible(i)) {
				int distance = game.getPathDistance(position, game.getCurGhostLoc(i));
				if (distance <= maxDistance) {
					count++;
				}
			}
		}
		return count;
	}
}
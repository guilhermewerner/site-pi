
package com.mycompany.mavenproject1;

import java.util.Set;
import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.PathExecutorState;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.visibility.Visibility;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.visibility.VisibilityCreator;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;

/**
 * Example of Pogamut Bot that is utilizing {@link Visibility} module.
 * 
 * Must be run on DM-1on1-Albatross. If you want to use different map
 * read javadoc for {@link Visibility} and {@link VisibilityCreator}.
 *
 * @author Jakub Gemrot aka Jimmy
 */
@AgentScoped
public class HideBot extends UT2004BotModuleController {

    /**
     * Taboo set is working as "black-list", that is you might add some
     * NavPoints to it for a certain time, marking them as "unavailable".
     */
    protected TabooSet<NavPoint> tabooNavPoints;
    
    /**
     * Used for autofixing navpoints.
     */
	protected UT2004PathAutoFixer autoFixer;
    
    /**
     * Here we can modify initializing command for our bot.
     *
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        return new Initialize().setName("HideBot");
    }

    /**
     * The bot is initialized in the environment - a physical representation of
     * the bot is present in the game.
     *
     * @param config information about configuration
     * @param init information about configuration
     */
    @SuppressWarnings("unchecked")
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange config, InitedMessage init) {
        // initialize taboo set where we store temporarily unavailable navpoints
        tabooNavPoints = new TabooSet<NavPoint>(bot);

        // auto-removes wrong navigation links between navpoints
        autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder);

        // IMPORTANT
        // adds a listener to the path executor for its state changes, it will allow you to 
        // react on stuff like "PATH TARGET REACHED" or "BOT STUCK"
        navigation.getPathExecutor().getState().addStrongListener(new FlagListener<IPathExecutorState>() {

            @Override
            public void flagChanged(IPathExecutorState changedValue) {
                pathExecutorStateChange(changedValue.getState());
            }
        });
    }

    Player hidingFrom;
    
    Heatup hiding = new Heatup(10000);

    Cooldown turning = new Cooldown(1200);
    
    NavPoint targetNavPoint;

	Cooldown changeNavPoint = new Cooldown(2000);
    
    @Override
    public void logic() {
    	
    	if (!visibility.isInitialized()) {
    		log.warning("Could not use VISIBILITY module :-(");
    		return;
    	}
    	
    	Player visible = players.getNearestVisiblePlayer();
    	
    	if (hidingFrom != null) {
    		
    		if (visible != hidingFrom) {
    			if (!hidingFrom.isVisible()) {
    				// SWITCH ATTENTION
        			startRunningAwayFrom(visible);
        			return;
    			}
    			
    			if (info.getLocation().getDistance(visible.getLocation()) < info.getLocation().getDistance(hidingFrom.getLocation())) {
    				// SWITCH ATTENTION
    				startRunningAwayFrom(hidingFrom);
    				return;
    			}
    		}
    		
    		continueRunningAwayFrom();
    		return;
    	}
    	    	
    	if (visible != null) {
    		startRunningAwayFrom(visible);
    		return;
    	}
    	
    	if (navigation.isNavigating()) {
    		navigation.stopNavigation();
    		navigation.setFocus(null);
    	}
    	
    	if (turning.tryUse()) {
    		move.turnHorizontal(110);
    	}
    	
    }

	private void startRunningAwayFrom(Player enemy) {
		this.hidingFrom = enemy;
		NavPoint cover = getCoverNavPoint(enemy);
		
		if (cover == null) {
			log.warning("No suitable navpoint, standing still... :-(");
			return;
		}
    	
    	navigation.setFocus(enemy);
    	runTo(cover);		
	}
	
    private NavPoint getCoverNavPoint(ILocated enemy) {
    	Set<NavPoint> navPoints = visibility.getCoverNavPointsFrom(enemy);
    	
    	NavPoint cover = DistanceUtils.getNearestFiltered(navPoints, info.getLocation(), tabooNavPoints);
    	
    	if (cover != null) return cover;
    	
    	log.warning("Could not use any navpoint as cover... trying random one.");
    	return getRandomNavPoint();
	}

	private void continueRunningAwayFrom() {
		if (hidingFrom == null) {
			log.warning("hidingFrom == null ???");
			return;
		}
		
		if (navigation.isNavigating()) {
			NavPoint suitableNavPoint = getCoverNavPoint(hidingFrom);
			if (targetNavPoint != suitableNavPoint) {
				if (changeNavPoint.tryUse()) {
					runTo(suitableNavPoint);
				}
			}
			return;
		} 
		
		// NOT NAVIGATING!
		if (targetNavPoint != null) {
			tabooNavPoints.add(targetNavPoint, 30);
		}
		
		if (hidingFrom.isVisible()) {
			startRunningAwayFrom(hidingFrom);
			return;
		}
		
		// NOT NAVIGATING! Player not visible...
		this.hidingFrom = null;
	}


	private void runTo(NavPoint navPoint) {
		log.info("Running to: " + navPoint);
		targetNavPoint = navPoint;
		navigation.navigate(targetNavPoint);
	}

	/**
     * Called each time our bot die. Good for reseting all bot state dependent variables.
     *
     * @param event
     */
    @Override
    public void botKilled(BotKilled event) {
        navigation.stopNavigation();
        targetNavPoint = null;
        hidingFrom = null;
        hiding.clear();
        turning.clear();
        changeNavPoint.clear();
    }

    /**
     * Path executor has changed its state (note that {@link UT2004BotModuleController${symbol_pound}getPathExecutor()}
     * is internally used by
     * {@link UT2004BotModuleController${symbol_pound}getNavigation()} as well!).
     *
     * @param state
     */
    protected void pathExecutorStateChange(PathExecutorState state) {
        switch (state) {
            case PATH_COMPUTATION_FAILED:
                // if path computation fails to whatever reason, just try another navpoint
                // taboo bad navpoint for 3 minutes
                tabooNavPoints.add(targetNavPoint, 180);
                break;
                
            case TARGET_REACHED:
            	tabooNavPoints.add(targetNavPoint, 5);
            	break;

            case STUCK:
                // the bot has stuck! ... target nav point is unavailable currently
                tabooNavPoints.add(targetNavPoint, 60);
                break;

            case STOPPED:
                // path execution has stopped
                targetNavPoint = null;
                break;
        }
    }

    /**
     * Randomly picks some navigation point to head to.
     *
     * @return randomly choosed navpoint
     */
    protected NavPoint getRandomNavPoint() {
        log.info("Picking new target navpoint.");

        // choose one feasible navpoint (== not belonging to tabooNavPoints) randomly
        NavPoint chosen = MyCollections.getRandomFiltered(getWorldView().getAll(NavPoint.class).values(), tabooNavPoints);

        if (chosen != null) {
            return chosen;
        }

        log.warning("All navpoints are tabooized at this moment, choosing navpoint randomly!");

        // ok, all navpoints have been visited probably, try to pick one at random
        return MyCollections.getRandom(getWorldView().getAll(NavPoint.class).values());
    }

    public static void main(String args[]) throws PogamutException {
        // wrapped logic for bots executions, suitable to run single bot in single JVM

        // we're forcingly setting logging to aggressive level FINER so you can see (almost) all logs 
        // that describes decision making behind movement of the bot as well as incoming environment events
        new UT2004BotRunner(HideBot.class, "HideBot").setMain(true).setLogLevel(Level.INFO).startAgent();
    }
}

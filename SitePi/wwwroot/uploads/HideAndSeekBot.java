
package com.mycompany.mavenproject1;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.PathExecutorState;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ControlMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.hideandseek.bot.UT2004BotHSController;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.HSBotState;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.HSScoreChangeReason;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSAssignSeeker;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSBotStateChanged;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSPlayerScoreChanged;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRoundEnd;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRoundStart;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRoundState;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRunnerCaptured;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRunnerFouled;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRunnerSafe;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRunnerSpotted;
import cz.cuni.amis.pogamut.ut2004.hideandseek.protocol.messages.HSRunnerSurvived;
import cz.cuni.amis.pogamut.ut2004.hideandseek.server.UT2004HSServer;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;

/**
 * Example of the bot playing HIDE-AND-SEEK-GAME according to {@link ControlMessage}s from {@link UT2004HSServer}. 
 *
 * @author Jakub Gemrot aka Jimmy
 */
@AgentScoped
public class HideAndSeekBot extends UT2004BotHSController<UT2004Bot> {

	private static String[] names = new String[]{"Peter", "James", "Johnny", "Craig", "Jimmy", "Steve", "Ronnie", "Bobby"};
	
	static {
		List<String> n = MyCollections.toList(names);
		Collections.shuffle(n);
		names = n.toArray(new String[n.size()]);
	}
	
	/**
	 * Just for the numbering of bots.
	 */
	private static int number = 0;
	
	/**
     * Taboo set is working as "black-list", that is you might add some
     * NavPoints to it for a certain time, marking them as "unavailable".
     */
    protected TabooSet<NavPoint> tabooNavPoints;
	
    /**
     * Path auto fixer watches for navigation failures and if some navigation
     * link is found to be unwalkable, it removes it from underlying navigation
     * graph.
     *
     * Note that UT2004 navigation graphs are some times VERY stupid or contains
     * VERY HARD TO FOLLOW links...
     */
    protected UT2004PathAutoFixer autoFixer;

	/**
     * Here we can modify initializing command for our bot.
     *
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        return new Initialize().setName(names[(++number) % names.length]);
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
        
        navigation.getLog().setLevel(Level.INFO);
        
//        ((SocketConnection)bot.getEventBus().getComponent(SocketConnection.COMPONENT_ID)).setLogMessages(true);
//        bot.getLogger().getCategory(SocketConnection.COMPONENT_ID.getToken()).setLevel(Level.ALL);
//        bot.getLogger().getCategory(YylexParser.COMPONENT_ID.getToken()).setLevel(Level.ALL);
    }

    /**
     * The bot is initialized in the environment - a physical representation of
     * the bot is present in the game.
     *
     * @param config information about configuration
     * @param init information about configuration
     */
    @Override
    public void botFirstSpawn(GameInfo gameInfo, ConfigChange config, InitedMessage init, Self self) {
        // change to ALL to receive logs from the navigation so you can get a grasp on how it is working
        navigation.getPathExecutor().getLog().setLevel(Level.WARNING);
    }
    
    // --------------------
    // ====================
    // HIDE AND SEEK EVENTS
    // ====================
    // --------------------
    
    /**
	 * Some RUNNER has just survived the round.
	 * <p><p>
	 * This event is triggered at the end of the round for every RUNNER that has not been FOULED, CAPTURED and did not make it to SAFE area.
	 * 
	 * @param event
	 */
    @Override
	protected void hsRunnerSurvived(HSRunnerSurvived event, boolean me) {
	}

	/**
	 * Some RUNNER has just been spotted by the SEEKER.
	 * @param event
	 */
    @Override
	protected void hsRunnerSpotted(HSRunnerSpotted event, boolean me) {
	}

	/**
	 * Some RUNNER has just made it to the safe-area.
	 * @param event
	 */
    @Override
	protected void hsRunnerSafe(HSRunnerSafe event, boolean me) {		
	}

	/**
	 * Some RUNNER has just been fouled-out because it stepped into the restricted-area while it was activated.
	 * @param event
	 * @param me whether this event speaks about your bot
	 */
    @Override
	protected void hsRunnerFouled(HSRunnerFouled event, boolean me) {
	}

	/**
	 * Some RUNNER has just been captured by the SEEKER, i.e., seeker has spotted the runner and made it to the safe-area before
	 * the runner.
	 * 
	 * @param event
	 * @param me whether this event speaks about your bot
	 */
    @Override
	protected void hsRunnerCaptured(HSRunnerCaptured event, boolean me) {
	}

	/**
	 * Round state update has been received.
	 * @param event
	 */
    @Override
	protected void hsRoundState(HSRoundState event) {
	}

	/**
	 * New round has just started, you may use this event to initialize your data-structures.
	 * @param event
	 */
    @Override
	protected void hsRoundStart(HSRoundStart event) {
		hideNavPoint = null;
		targetNavPoint = null;
		goingToFoul = random.nextInt(6) == 5; // 1/6 probability that the RUNNER will perform FOUL BEHAVIOR, will remain in restricted area instead of hiding
		tabooNavPoints.clear();		
		navigation.stopNavigation(); // prevent commands from previous round to be still effective
	}

	/**
	 * Round has just ended, you may use this event to cleanup your data-structures.
	 * @param event
	 */
    @Override
	protected void hsRoundEnd(HSRoundEnd event) {    	
	}

	/**
	 * Some player (runner/seeker) score has just changed, you may examine the reason within {@link HSPlayerScoreChanged${symbol_pound}getScoreChangeReason()}, see
	 * {@link HSScoreChangeReason}.
	 * 
	 * @param event
	 * @param me whether this event speaks about your bot
	 */
    @Override
	protected void hsPlayerScoreChanged(HSPlayerScoreChanged event, boolean me) {
	}

    /**
	 * Some bot {@link HSBotState} has been updated.
	 * @param event
	 * @param me whether this event speaks about your bot
	 */
	protected void hsBotStateChanged(HSBotStateChanged event, boolean me) {		
	}
    
	/**
	 * SEEKER role has been just assigned to some bot.
	 * @param event
	 * @param me whether this event speaks about your bot
	 */
    @Override
	protected void hsAssignSeeker(HSAssignSeeker event, boolean me) {	
	}
	
	// -----
	// =====
	// LOGIC
	// =====
	// -----

    /**
     * This method is called only once right before actual logic() method is
     * called for the first time.
     */
    @Override
    public void beforeFirstLogic() {
    }

    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in GameBots ini
     * file in UT2004/System folder.
     */
    @Override
    public void logic() {
        // mark that another logic iteration has began
        log.info("====== LOGIC ITERATION ======");
        
        // UNCOMMENT TO GET DETAIL INFO DURING RUN-TIME
        // logState();
        
        if (!hide.isRoundRunning()) return;
        
        if (!hide.isMeAlive()) return;
        
        switch(hide.getMyState()) {
        case RUNNER:
        	logicRunner();
        	break;
        case RUNNER_SPOTTED:
        	logicRunnerSpotted();
        	break;
        case SEEKER:
        	logicSeeker();
        	break;
        default:
        	// SHOULD NOT REACH HERE, BUT LET'S PLAY SAFE...
        	return;
        }
    }
    
    private void logState() {
    	log.info("------      STATE      ------");
    	log.info("Game state:                  " + hide.getGameState());
    	log.info("Game running:                " + hide.isGameRunning());
    	if (!hide.isGameRunning()) return;
    	log.info("Round running:               " + hide.isRoundRunning());
    	if (!hide.isRoundRunning()) return;
    	log.info("Me:                          " + hide.getMyState());
    	log.info("Me spawned?                  " + hide.isMeAlive());
    	log.info("Hiding time left:            " + hide.getRemainingHidingTime());
    	log.info("Restricted area time left:   " + hide.getRemainingRestrictedAreaTime());
    	log.info("Round time left:             " + hide.getRemainingRoundTime());
    	log.info("Distance to safe-area:       " + hide.getMySafeAreaDistance()); 
    	log.info("Distance to restricted-area: " + hide.getMyRestrictedAreaDistance());
    }
    
    // ------------
    // ============
    // LOGIC RUNNER
    // ============
    // ------------
    
    NavPoint hideNavPoint;
    
    Boolean goingToFoul = null;
    
    private void logicRunner() {
    	log.info("------ RUNNER ------");
    	if (hide.isHidingTime()) {
    		if (goingToFoul) {
    			log.info("GOING TO FOUL! Standing-stil...");
    			return;
    		}
    		log.info("HIDING!");    		
    		if (hideNavPoint == null) {
    			hideNavPoint = chooseHideNavPoint();
    			if (hideNavPoint == null) {
    				log.warning("THERE IS NO SUITABLE HIDE-POINT TO RUN TO!");
    				return;
    			}
    		}
    		if (info.isAtLocation(hideNavPoint)) {
    			if (navigation.isNavigating()) {
    				navigation.stopNavigation();
    			}
    			log.info("HIDDEN!");
    			move.turnHorizontal(30);
    			return;
    		}
    		navigation.navigate(hideNavPoint);
    		return;
    	} 
    	
    	if (hide.isRestrictedAreaActivated()) {
    		log.info("RESTRICTED AREA ACTIVE, standing still!");
    		return;
    	}
    	
    	runToSafePoint();	    	
    }
    
    private NavPoint chooseHideNavPoint() {
    	if (visibility.isInitialized()) {
    		// WE HAVE VISIBILITY INFORMATION!
    		// => lets get smarty!
    		return 
    			MyCollections.getRandomFiltered(
    				visibility.getCoverNavPointsFrom(hide.getSafeArea()),
    				new IFilter<NavPoint>() {
						@Override
						public boolean isAccepted(NavPoint object) {
							return !hide.isInRestrictedArea(object, 50);
						}
					}
    			);
    	}
		// NO VISIBILITY INFORMATION PROVIDED, CHOOSE RANDOM SUITABLE
    	return
	    	MyCollections.getRandomFiltered(
		    	navPoints.getNavPoints().values(), 
		    	new IFilter<NavPoint>() {
					@Override
					public boolean isAccepted(NavPoint object) {
						return !hide.isInRestrictedArea(object, 50);
					}
				}			    	
		    );    	
    }

    // --------------------
    // ====================
    // LOGIC RUNNER SPOTTED
    // ====================
    // --------------------
    
    private void logicRunnerSpotted() {    	
    	runToSafePoint();
    }
    
    private void runToSafePoint() {
    	log.info("RUNNING TO SAFE POINT");
    	navigation.navigate(hide.getSafeArea());
    }
    
    // ------------
    // ============
    // LOGIC SEEKER
    // ============
    // ------------
    
    private void logicSeeker() {   
    	log.info("------ SEEKER ------");
    	if (hide.canCaptureRunner()) {
    		log.info("CAN CAPTURE RUNNER!");
    		runToSafePoint();
    	} else {
    		if (players.canSeePlayers()) {
    			targetNavPoint = null;
    			handlePlayerNavigation(players.getNearestVisiblePlayer());
    		} else {
    			handleNavPointNavigation();
    		}
    	}
    }
    
    private NavPoint targetNavPoint;

    private void handleNavPointNavigation() {
        if (navigation.isNavigatingToNavPoint()) {
            // IS TARGET CLOSE & NEXT TARGET NOT SPECIFIED?
            while (navigation.getContinueTo() == null && navigation.getRemainingDistance() < 400) {
                    // YES, THERE IS NO "next-target" SET AND WE'RE ABOUT TO REACH OUR TARGET!
                    navigation.setContinueTo(getRandomNavPoint());
                    // note that it is WHILE because navigation may immediately eat up "next target" and next target may be actually still too close!
            }

            // WE'RE NAVIGATING TO SOME NAVPOINT
            return;
        }

        // NAVIGATION HAS STOPPED ... 
        // => we need to choose another navpoint to navigate to
        // => possibly follow some players ...

        targetNavPoint = getRandomNavPoint();
        if (targetNavPoint == null) {
            log.severe("COULD NOT CHOOSE ANY NAVIGATION POINT TO RUN TO!!!");
            if (world.getAll(NavPoint.class).size() == 0) {
                log.severe("world.getAll(NavPoint.class).size() == 0, there are no navigation ponits to choose from! Is exporting of nav points enabled in GameBots2004.ini inside UT2004?");
            }
            config.setName("NavigationBot [CRASHED]");
            return;
        }

        navigation.navigate(targetNavPoint);
    }
    
    private void handlePlayerNavigation(Player player) {    	
        if (navigation.isNavigating() && navigation.getCurrentTargetPlayer() == player) {
            // WE'RE NAVIGATING TO SOME PLAYER
            return;
        }

        // START THE NAVIGATION
        navigation.navigate(player);
    }

    /**
     * Called each time our bot die. Good for reseting all bot state dependent
     * variables.
     *
     * @param event
     */
    @Override
    public void botKilled(BotKilled event) {
        navigation.stopNavigation();
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
                // taboo bad navpoint for 10 seconds
                tabooNavPoints.add(targetNavPoint, 10);
                break;

            case TARGET_REACHED:
                // taboo reached navpoint for 10 seconds
                tabooNavPoints.add(targetNavPoint, 10);
                break;

            case STUCK:
                // the bot has stuck! ... target nav point is unavailable currently
                tabooNavPoints.add(targetNavPoint, 30);
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
        NavPoint chosen = MyCollections.getRandomFiltered(navPoints.getNavPoints().values(), tabooNavPoints);

        if (chosen != null) {
            return chosen;
        }

        log.warning("All navpoints are tabooized at this moment, choosing navpoint randomly!");

        // ok, all navpoints have been visited probably, try to pick one at random
        return MyCollections.getRandom(navPoints.getNavPoints().values());
    }

    public static void main(String args[]) throws PogamutException {
    	// Starts 1 bot
    	// If you want to execute the whole Hide&Seek game/match, execute {@link HideAndSeekGame}.
        new UT2004BotRunner(HideAndSeekBot.class, "HSBot").setMain(true).setLogLevel(Level.WARNING).startAgents(1);       
    }
}

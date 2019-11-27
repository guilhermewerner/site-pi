
package com.mycompany.mavenproject1;

/*
 * AffectEngine.java
 *
 * Copyright (c) 2005, 2006, 2007, 2008, Patrick Gebhard, DFKI GmbH
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of the DFKI GmbH nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import de.affect.emotion.Emotion;
import de.affect.emotion.EmotionType;
import de.affect.manage.AffectManager;
import de.affect.manage.event.AffectUpdateEvent;
import de.affect.manage.event.AffectUpdateListener;
import de.affect.mood.Mood;
import de.affect.xml.AffectOutputDocument;

import de.affect.xml.AffectInputDocument.AffectInput;
import de.affect.xml.AffectInputDocument.AffectInput.Character;
import de.affect.xml.AffectInputDocument.AffectInput.BasicEEC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the basic interface between Pogamut and ALMA.
 *
 * This code is based on the AffectEngine.java class provided with ALMA as an example
 * implementation.
 *
 * @author Patrick Gebhard, Michal Bida
 */
public class ALMA implements AffectUpdateListener {

    /** The agent this PogamutALMA instance is for. */
    public EmotionalBot myAgent;
    /** The ALMA Java implementation */
    public static AffectManager fAM = null;

    /** ALMA affect computation definition file */
    private static String sALMACOMP = "./conf/AffectComputation.aml";
    
    /** ALMA character definition file */
    private static String sALMADEF = "./conf/CharacterDefinition.aml";
    
    /** Current level time in seconds (contains also milliseconds after dot) */
    public double currentTime;

    /* ALMA mode:
    false - output on console
    true - graphical user interface CharacterBuilder
    NOTE: No runtime windows (defined in AffectComputation or
    AffectDefinition will be displayed!) */
    private static final boolean sGUIMode = false;
    /** Console logging */
    public static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * The constructor that sets up ALMA engine.
     *
     * @param agent
     */
    public ALMA(EmotionalBot agent) {
        // Starting the ALMA affect engine
        myAgent = agent;

        try {

            //fAM = new AffectManager(sALMACOMP, sALMADEF, sGUIMode);
            fAM = new AffectManager(ALMA.class.getClassLoader().getResourceAsStream(sALMACOMP), ALMA.class.getClassLoader().getResourceAsStream(sALMADEF), sGUIMode);
            //disable alma logging
            fAM.log.setLevel(Level.OFF);
            fAM.addAffectUpdateListener(this);

        } catch (IOException io) {
            log.info("Error during ALMA initialisation");
            io.printStackTrace();
            System.exit(-1);
        } catch (Exception xmle) {
            log.info("Error in ALMA configuration");
            xmle.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Listens to affect updates computed by ALMA. This implements the AffectUpdateListener.
     *
     * Called each time something in ALMA is changed. In ALMA the affects are changing
     * very often resulting in this method getting called very often as well.
     * 
     * @param event
     */
    public synchronized void update(AffectUpdateEvent event) {
        AffectOutputDocument aod = event.getUpdate();
        //nothing for now, logging?
    }

    /**
     * Returns current mood for target agent.
     *
     * @param agentName
     * @return
     */
    public Mood getCurrentMood(String agentName) {
        return fAM.getCharacterByName(agentName).getCurrentMood();
    }

    /**
     * Gets all emotions for target agent, that are felt toward the elicitor provided.
     *
     * @param agentName
     * @param elicitor
     * @return
     */
    public List<Emotion> getAllEmotionsForElicitor(String agentName, String elicitor) {

        ArrayList<Emotion> emotionList = new ArrayList<Emotion>();
        Emotion tempEm = null;

        for (int j = 0; j < EmotionType.values().length; j++) {

            //we get emotions one by one from ALMA history
            tempEm = fAM.getCharacterByName(agentName).getEmotionHistory().getEmotionByElicitor(EmotionType.values()[j], elicitor);
            if (tempEm != null) {
                emotionList.add(tempEm);
            }
        }

        return emotionList;
    }

    /**
     * Gets emotion for target agent name of input type and for input elicitor.
     *
     * @param agentName
     * @param elicitor
     * @param type
     * @return
     */
    public Emotion getEmotionForElicitor(String agentName, String elicitor, EmotionType type) {
        return fAM.getCharacterByName(agentName).getEmotionHistory().getEmotionByElicitor(type, elicitor);
    }

    /**
     * Gets dominant emotion for the input agent.
     *
     * @param agentName
     * @return
     */
    public Emotion getDominantEmotion(String agentName){
    	return fAM.getCharacterByName(agentName).getCurrentEmotions().getDominantEmotion();
    }

    /**
     * Gets the ALMA current emotions (in characters focus) for target agent.
     *
     * @param agentName
     * @return
     */
    public List<Emotion> getCurrentEmotions(String agentName) {
        if (fAM.getCharacterByName(agentName) != null) {
            return fAM.getCharacterByName(agentName).getCurrentEmotions().getEmotions();
        } else {
            return new ArrayList<Emotion>();
        }
    }

    /**
     * Creates an AffectInput document containing an BasicEEC Element and returns a AffectInput object
     *
     * @param actor
     * @param desirability
     * @param agency
     * @param praiseworthiness
     * @param appealingness
     * @param liking
     * @param likelihood
     * @param elicitor
     * @param realization
     * @return
     */
    public AffectInput createAffectInputBasicEEC(String actor,
            double desirability, double praiseworthiness, double appealingness, double likelihood,
            double liking, double realization, String elicitor, String agency) {

        AffectInput aiInput = AffectInput.Factory.newInstance();
        // Building the Character element
        Character perfCharacter = Character.Factory.newInstance();
        perfCharacter.setName(actor);

        BasicEEC eec = BasicEEC.Factory.newInstance();

        eec.setDesirability(desirability);
        eec.setPraiseworthiness(praiseworthiness);
        eec.setAppealingness(appealingness);
        eec.setLikelihood(likelihood);
        eec.setLiking(liking);
        eec.setRealization(realization);
        eec.setAgency((agency.toLowerCase() == "self") ? BasicEEC.Agency.SELF : BasicEEC.Agency.OTHER);
        eec.setElicitor(elicitor);

        aiInput.setCharacter(perfCharacter);
        aiInput.setBasicEEC(eec);

        return aiInput;
    }

    /**
     * Creates an AffectInput document containing an BasicEEC Element and returns a AffectInput object
     * This type eec element is constructed elsewhere
     * @param actor
     * @param eec
     * @return
     */
    public AffectInput createAffectInput(String actor, BasicEEC eec) {

        AffectInput aiInput = AffectInput.Factory.newInstance();
        // Building the Character element
        Character perfCharacter = Character.Factory.newInstance();
        perfCharacter.setName(actor);

        aiInput.setCharacter(perfCharacter);
        aiInput.setBasicEEC(eec);

        return aiInput;
    }

    /**
     * The <code>processAffectInput</code>  passes instance of AffectInput to AffectManager
     * event string is here just for logging
     * @param ai
     * @param event
     */
    public void processAffectInput(AffectInput ai, String event) {

        myAgent.getLog().warning("Name: " + ai.getCharacter().getName() + " Event: " + event + " ${symbol_escape}n" + myAgent.currentTime +
                " Vars: D: " + ai.getBasicEEC().getDesirability() + "; P: " + ai.getBasicEEC().getPraiseworthiness() +
                "; A: " + ai.getBasicEEC().getAppealingness() + "; Lh: " + ai.getBasicEEC().getLikelihood() +
                "; Li: " + ai.getBasicEEC().getLiking() + "; R: " + ai.getBasicEEC().getRealization() +
                "; Ag: " + ai.getBasicEEC().getAgency() + "; El.: " + ai.getBasicEEC().getElicitor());

        fAM.processSignal(ai);
    }
}



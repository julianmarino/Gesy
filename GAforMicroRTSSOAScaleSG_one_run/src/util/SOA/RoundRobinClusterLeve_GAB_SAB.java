/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.SOA;

import Standard.StrategyTactics;
import ai.CMAB.A3NWithin;
import ai.CMAB.CMABBuilder;
import ai.CMAB.CmabNaiveMCTS;
import static ai.CMAB.GameVisualSimulationCMAB.decodeScripts;
import ai.RandomBiasedAI;
import ai.abstraction.combat.KitterDPS;
import ai.abstraction.combat.NOKDPS;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.PORangedRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.ahtn.AHTNAI;
import ai.aiSelection.AlphaBetaSearch.AlphaBetaSearch;
import ai.asymmetric.GAB.SandBox.GAB;
import ai.asymmetric.GAB.SandBox.GABScriptChoose;
import ai.asymmetric.PGS.LightPGSSCriptChoice;
import ai.asymmetric.PGS.NGS;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.asymmetric.PGS.PGSSCriptChoice;
import ai.asymmetric.PGS.PGSSCriptChoiceRandom;
import ai.asymmetric.PGS.PGSmRTS;
import ai.asymmetric.SAB.SAB;
import ai.asymmetric.SAB.SABScriptChoose;
import ai.asymmetric.SSS.LightSSSmRTSScriptChoice;
import ai.asymmetric.SSS.SSSmRTS;
import ai.asymmetric.SSS.SSSmRTSScriptChoice;
import ai.asymmetric.SSS.SSSmRTSScriptChoiceRandom;
import ai.cluster.CABA;
import ai.cluster.CABA_Enemy;
import ai.cluster.CABA_TDLearning;
import ai.cluster.CIA_Enemy;
import ai.cluster.CIA_PlayoutTemporal;
import ai.cluster.CIA_TDLearning;
import ai.competition.IzanagiBot.Izanagi;
import ai.competition.capivara.Capivara;
import ai.competition.capivara.CmabAssymetricMCTS;
import ai.competition.tiamat.Tiamat;
import ai.configurablescript.BasicExpandedConfigurableScript;
import ai.configurablescript.ScriptsCreator;
import ai.evaluation.LTD2;
import ai.evaluation.PlayoutFunction;
import ai.evaluation.SimpleEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.believestatemcts.BS3_NaiveMCTS;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.naivemcts.NaiveMCTSNoGamma;
import ai.mcts.uct.UCT;
import ai.minimax.ABCD.ABCD;
import ai.montecarlo.MonteCarlo;
import ai.montecarlo.lsi.LSI;
import ai.puppet.PuppetSearchMCTS;
import ai.scv.SCV;
import ai.scv.SCVPlus;
import ai.utalca.UTalcaBot;
import gui.PhysicalGameStatePanel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTYpeTableBattle;
import rts.units.UnitTypeTable;

/**
 *
 * @author rubens Classe respons??vel por rodar os confrontos entre duas IA's.
 * Ambiente totalmente observ??vel.
 */
public class RoundRobinClusterLeve_GAB_SAB {
    
    static HashMap<Integer, ArrayList<Integer>>  mapElements = new HashMap<>();

    public boolean run(String sIA1, String sIA2, String sMap, String sIte, String pathLog) throws Exception {
        int iAi1 = Integer.parseInt(sIA1);
        int iAi2 = Integer.parseInt(sIA2);
        int map = Integer.parseInt(sMap);

        ArrayList<String> log = new ArrayList<>();
        //controle de tempo
        Instant timeInicial = Instant.now();
        Duration duracao;

        List<String> maps = new ArrayList<>(Arrays.asList(             
                "maps/8x8/basesWorkers8x8A.xml",
                "maps/8x8/FourBasesWorkers8x8.xml",
                "maps/16x16/basesWorkers16x16A.xml",
                "maps/16x16/TwoBasesBarracks16x16.xml",
                
                "maps/24x24/basesWorkers24x24A.xml",
                "maps/24x24/basesWorkers24x24A_Barrack.xml",
                "maps/DoubleGame24x24.xml",
                "maps/32x32/basesWorkers32x32A.xml",
                "maps/32x32/basesWorkersBarracks32x32.xml",
                "maps/BWDistantResources32x32.xml",
                "maps/BroodWar/(4)BloodBath.scmB.xml",
                "maps/BroodWar/(4)BloodBath.scmD.xml"
                
        ));

        //UnitTypeTable utt = new UnitTYpeTableBattle();
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load(maps.get(map), utt);

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 14000;
        int PERIOD = 20;
        boolean gameover = false;

        if (pgs.getHeight() == 8) {
            MAXCYCLES = 8000;
        } else if (pgs.getHeight() == 16) {
            MAXCYCLES = 10000;
        } else if (pgs.getHeight() == 24) {
            MAXCYCLES = 12000;
        } else if (pgs.getHeight() == 32) {
            MAXCYCLES = 12000;
        } else if (pgs.getHeight() == 64) {
            MAXCYCLES = 15000;
        }

        generateConfig();
        List<AI> ais = new ArrayList<>();

        AI ai1;
        AI ai2;
        if(iAi1 == 0){
            //ai1 = new PGSmRTS(utt);
            //ai1 = new PGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), "PGS");
            //ai1 = new LightPGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "PGS");
            //ai1 = new LightSSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "SSS");
            //ai1 = new SSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), "SSS");
            //ai1 = new CMABBuilder(100, -1, 200, 10, 0, new RandomBiasedAI(utt), new SimpleSqrtEvaluationFunction3(), 0, utt, new ArrayList<>(), "CmabCombinatorialGenerator");
            ai1 = new CmabNaiveMCTS(100, -1, 200, 10, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(utt),
                        new SimpleSqrtEvaluationFunction3(), true, "CmabCombinatorialGenerator", utt,
                        RoundRobinClusterLeve.decodeScripts(utt, "0;1;2;3;"), "A1N");
            ai2 = getIA(utt,iAi2);
        }else{
            //ai2 = new PGSmRTS(utt);
            //ai2 = new PGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), "PGS");
            //ai2 = new LightPGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "PGS");
            //ai2 = new LightSSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "SSS");
            //ai2 = new SSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), "SSS");
            //ai2 = new CMABBuilder(100, -1, 200, 10, 0, new RandomBiasedAI(utt), new SimpleSqrtEvaluationFunction3(), 0, utt, new ArrayList<>(), "CmabCombinatorialGenerator");;
            ai2 = new CmabNaiveMCTS(100, -1, 200, 10, 0.3f, 0.0f, 0.4f, 0, new RandomBiasedAI(utt),
                        new SimpleSqrtEvaluationFunction3(), true, "CmabCombinatorialGenerator", utt,
                        RoundRobinClusterLeve.decodeScripts(utt, "0;1;2;3;"), "A1N");
            ai1 = getIA(utt,iAi2);
        }
        
        ais.clear();
        

        /*
            Vari??veis para coleta de tempo
         */
        double ai1TempoMin = 9999, ai1TempoMax = -9999;
        double ai2TempoMin = 9999, ai2TempoMax = -9999;
        double sumAi1 = 0, sumAi2 = 0;
        int totalAction = 0;

        log.add("---------AIs---------");
        log.add("AI 1 = " + ai1.toString());
        log.add("AI 2 = " + ai2.toString() + "\n");

        log.add("---------Mapa---------");
        log.add("Mapa= " + maps.get(map) + "\n");

        //m??todo para fazer a troca dos players
        //JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 840, 840, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);
        long startTime;
        long timeTemp;
        //System.out.println("Tempo de execu????o P2="+(startTime = System.currentTimeMillis() - startTime));
        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do {
            if (System.currentTimeMillis() >= nextTimeToUpdate) {
                totalAction++;
                startTime = System.currentTimeMillis();

                PlayerAction pa1 = ai1.getAction(0, gs);
                //dados de tempo ai1
                timeTemp = (System.currentTimeMillis() - startTime);
                sumAi1 += timeTemp;
                //coleto tempo m??nimo
                if (ai1TempoMin > timeTemp) {
                    ai1TempoMin = timeTemp;
                }
                //coleto tempo maximo
                if (ai1TempoMax < timeTemp) {
                    ai1TempoMax = timeTemp;
                }

                startTime = System.currentTimeMillis();
                PlayerAction pa2 = ai2.getAction(1, gs);
                //dados de tempo ai2
                timeTemp = (System.currentTimeMillis() - startTime);
                sumAi2 += timeTemp;
                //coleto tempo m??nimo
                if (ai2TempoMin > timeTemp) {
                    ai2TempoMin = timeTemp;
                }
                //coleto tempo maximo
                if (ai2TempoMax < timeTemp) {
                    ai2TempoMax = timeTemp;
                }

                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                //w.repaint();
                nextTimeToUpdate += PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //avaliacao de tempo
            duracao = Duration.between(timeInicial, Instant.now());

        } while (!gameover && (gs.getTime() < MAXCYCLES));

        log.add("Total de actions= " + totalAction + " sumAi1= " + sumAi1 + " sumAi2= " + sumAi2 + "\n");

        log.add("Tempos de AI 1 = " + ai1.toString());
        log.add("Tempo minimo= " + ai1TempoMin + " Tempo maximo= " + ai1TempoMax + " Tempo medio= " + (sumAi1 / (long) totalAction));

        log.add("Tempos de AI 2 = " + ai2.toString());
        log.add("Tempo minimo= " + ai2TempoMin + " Tempo maximo= " + ai2TempoMax + " Tempo medio= " + (sumAi2 / (long) totalAction) + "\n");

        log.add("Winner " + Integer.toString(gs.winner()));
        log.add("Game Over");

        if (gs.winner() == -1) {
            System.out.println("Empate!" + ai1.toString() + " vs " + ai2.toString() + " Max Cycles =" + MAXCYCLES + " Time:" + duracao.toMinutes());
        }

        gravarLog(log, sIA1, sIA2, sMap, sIte, pathLog);
        //System.exit(0);
        return true;
    }

    private void gravarLog(ArrayList<String> log, String sIA1, String sIA2, String sMap, String sIte, String pathLog) throws IOException {
        if (!pathLog.endsWith("/")) {
            pathLog += "/";
        }
        String nameArquivo = pathLog + "match_" + sIA1 + "_" + sIA2 + "_" + sMap + "_" + sIte + ".scv";
        File arqLog = new File(nameArquivo);
        if (!arqLog.exists()) {
            arqLog.createNewFile();
        }
        //abre o arquivo e grava o log
        try {
            FileWriter arq = new FileWriter(arqLog, false);
            PrintWriter gravarArq = new PrintWriter(arq);
            for (String l : log) {
                gravarArq.println(l);
            }

            gravarArq.flush();
            gravarArq.close();
            arq.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static List<AI> decodeScripts(UnitTypeTable utt, String sScripts) {
        
        //decomp??e a tupla
        ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
        String[] itens = sScripts.split(";");

        for (String element : itens) {
            iScriptsAi1.add(Integer.decode(element));
        }
        
        List<AI> scriptsAI = new ArrayList<>();

        ScriptsCreator sc = new ScriptsCreator(utt,300);
        ArrayList<BasicExpandedConfigurableScript> scriptsCompleteSet = sc.getScriptsMixReducedSet();

        iScriptsAi1.forEach((idSc) -> {
            scriptsAI.add(scriptsCompleteSet.get(idSc));
        });

        return scriptsAI;
    }
    
    public static void generateConfig(){
        int cont = 0;
        for (int i = 0; i <= 10; i++) {
            for (int j = 0; j < 9; j++) {
                 ArrayList<Integer>  choices = new ArrayList<>();
                 choices.add(0, i);
                 choices.add(1, j);
                mapElements.put(cont, choices);
                cont++;
                        
            }
        }
        //System.out.println("tests.ClusterTesteLeve_GAB_SAB.generateConfig() teste total ="+ mapElements.keySet().size());
        
    }

    private static AI getIA(UnitTypeTable utt, int iAi2) {
        ArrayList<Integer>  choices = mapElements.get(iAi2);
        //return new GABScriptChoose(utt, choices.get(0), choices.get(1));
        //return new SABScriptChoose(utt, choices.get(0), choices.get(1));
        //return new CMABBuilder(100, -1, 100, 1, 0, new RandomBiasedAI(), new SimpleSqrtEvaluationFunction3(), 0, utt,
        //        new ArrayList<>(), "CmabCombinatorialGenerator", getManager(choices.get(1)), choices.get(0));
        return new A3NWithin(100, -1, 100, 8, 0.3F, 0.0F, 0.4F, 0, new RandomBiasedAI(utt),
                        new SimpleSqrtEvaluationFunction3(), true, utt, getManager(choices.get(1)), choices.get(0),
                        decodeScripts(utt, "1;2;3;"), "A3Nw");
    }
    
     protected static String getManager(int idBehavior) {
        String ret;
        switch (idBehavior) {
            case 0:
                ret = "ManagerRandom";
                break;
            case 1:
                ret = "ManagerClosest";
                break;
            case 2:
                ret = "ManagerClosestEnemy";
                break;
            case 3:
                ret = "ManagerFather";
                break;
            case 4:
                ret = "ManagerFartherEnemy";
                break;
            case 5:
                ret = "ManagerLessLife";
                break;
            case 6:
                ret = "ManagerMoreLife";
                break;
            case 7:
                ret = "ManagerLessDPS";
                break;
            case 8:
                ret = "ManagerMoreDPS";
                break;
            default:
                ret = "ManagerRandom";
        }
        
        return ret;
    }
     
}

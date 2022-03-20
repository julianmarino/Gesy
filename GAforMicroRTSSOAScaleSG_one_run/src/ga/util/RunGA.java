package ga.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import ai.core.AI;
import ga.ScriptTableGenerator.ScriptsTable;
import ga.config.ConfigurationsGA;
import ga.model.Chromosome;
import ga.model.Population;
import ga.util.Evaluation.RatePopulation;
import ga.util.Evaluation.RoundRobinEliteandSampleEval;
import ga.util.Evaluation.RoundRobinEliteandSampleIterativeEval;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.SOA.RoundRobinTOScale_GP;
import util.SOA.SOARoundRobinTOScale_GP;
import util.sqlLite.Log_Facade;

public class RunGA {
	
	String curriculumportfolio;
	
	public RunGA(String curriculumportfolio)
	{
		this.curriculumportfolio=curriculumportfolio;
	}

	private Population population;
	private Instant timeInicial;
	private int generations = 0;
	private ScriptsTable scrTable;
	private HashMap<Chromosome, BigDecimal> eliteIndividuals=new HashMap<Chromosome, BigDecimal>();

	private final String pathTableScripts = System.getProperty("user.dir").concat("/Table/");
	private final String pathLogs = System.getProperty("user.dir").concat("/Tracking/");
	private final String pathInitialPopulation = System.getProperty("user.dir").concat("/InitialPopulation/");
	private final String pathUsedCommands = System.getProperty("user.dir").concat("/commandsUsed/");
	
	static int [] frequencyIdsRulesForUCB= new int[ConfigurationsGA.QTD_RULES];
	static int numberCallsUCB11=0;
	//private final String pathTableScripts = "/home/rubens/cluster/TesteNewGASG/Table/";

	/**
	 * Este metodo aplicará todas as fases do processo de um algoritmo Genético
	 * Fres
	 * @param evalFunction
	 *            Será a função de avaliação que desejamos utilizar
	 */
	public Population run(RoundRobinEliteandSampleEval evalFunction, String scriptsSetCover, HashSet<String> booleansUsed) {
		
		runmRTS();
		
		// Creating the table of scripts
		scrTable = new ScriptsTable(pathTableScripts);
		//do {
			if(ConfigurationsGA.portfolioSetCover)
			{
				scrTable = scrTable.generateScriptsTableFromSetCover(ConfigurationsGA.SIZE_TABLE_SCRIPTS,scriptsSetCover,booleansUsed,curriculumportfolio);
			}
			else
			{
				if(!ConfigurationsGA.recoverTable)
				{
					scrTable = scrTable.generateScriptsTable(ConfigurationsGA.SIZE_TABLE_SCRIPTS);
				}
				else
				{
					scrTable = scrTable.generateScriptsTableRecover();
				}
			}
		   //}while(scrTable.checkDiversityofTypes());
		scrTable.setCurrentSizeTable(scrTable.getScriptTable().size());

		PrintWriter f0;
		try {
			f0 = new PrintWriter(new FileWriter(pathLogs+"Tracking.txt"));

		do {
			// Fase 1 = gerar a população inicial
			if(!ConfigurationsGA.curriculum)
			{
				population = Population.getInitialPopulation(ConfigurationsGA.SIZE_POPULATION, scrTable);
			}
			else
			{
				population = Population.getInitialPopulationCurriculum(ConfigurationsGA.SIZE_POPULATION, scrTable, pathInitialPopulation);
			}			
			

			// Fase 2 = avalia a população
			evalFunction.setEliteIndividuals(eliteIndividuals);
			population = evalFunction.evalPopulation(population, this.generations, scrTable);			
			
//			population.printWithValue(f0);
//			System.out.println("sep");
		
			//Get all the used commands
			if(ConfigurationsGA.removeRules==true)
				System.out.println("Log - Generation before remotion = " + this.generations);
				f0.println("Log - Generation = " + this.generations);	
				population.printWithValue(f0);
				population.fillAllCommands(pathTableScripts);
//		    Iterator it = population.getAllCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		    }
			//Choose the used commands
			if(ConfigurationsGA.removeRules==true)
				population.chooseusedCommands(pathUsedCommands);
//		    Iterator it = population.getUsedCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		        //it.remove(); // avoids a ConcurrentModificationException
//		    }
			
			//Remove used commands from all commands
			if(ConfigurationsGA.removeRules==true)
				population.removeCommands(scrTable);
			
//		    Iterator it2 = population.getAllCommandsperGeneration().entrySet().iterator();
//		    while (it2.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it2.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		    }
			
			System.out.println("Log - Generation = " + this.generations);
			f0.println("Log - Generation = " + this.generations);
			population.printWithValue(f0);
			f0.flush();
		} while (resetPopulation(population));

		resetControls();
		// Fase 3 = critério de parada
		while (continueProcess()) {

			// Fase 4 = Seleção (Aplicar Cruzamento e Mutação)
			Selection selecao = new Selection();
			population = selecao.applySelection(population, scrTable, pathTableScripts);
			eliteIndividuals=selecao.eliteIndividuals;
			// Repete-se Fase 2 = Avaliação da população
			evalFunction.setEliteIndividuals(eliteIndividuals);
			population = evalFunction.evalPopulation(population, this.generations, scrTable);
			
			//Get all the used commands
			if(ConfigurationsGA.removeRules==true)
				population.fillAllCommands(pathTableScripts);
			
			//Remove the unused commands
			if(ConfigurationsGA.removeRules==true)
				population.chooseusedCommands(pathUsedCommands);
//		    Iterator it = population.getUsedCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		        //it.remove(); // avoids a ConcurrentModificationException
//		    }
			//Remove used commands from all commands
			if(ConfigurationsGA.removeRules==true)
				population.removeCommands(scrTable);

			// atualiza a geração
			updateGeneration();

			System.out.println("Log - Generation = " + this.generations);
			f0.println("Log - Generation = " + this.generations);
			population.printWithValue(f0);
			f0.flush();
			
			if(ConfigurationsGA.UCB1==true)
			{
				Log_Facade.shrinkRewardTable();
				System.out.println("call shrink");
			}
		}
		
		f0.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return population;
	}

	public void runmRTS() {
		try {
			System.out.print("already running");
			String jarFile=System.getProperty("user.dir").concat("/mRTS.jar");
			System.out.println("path jar "+jarFile);
			Process proc = Runtime.getRuntime().exec("java -jar \""+jarFile+"\"");
			InputStream in = proc.getInputStream();
			InputStream err = proc.getErrorStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void runMainGame(String tupleAi1, String tupleAi2) throws Exception {
	       ArrayList<String> log = new ArrayList<>();
	        //controle de tempo
	        Instant timeInicial = Instant.now();
	        Duration duracao;

	        log.add("Tupla A1 = " + tupleAi1);
	        log.add("Tupla A2 = " + tupleAi2);
	        

	        List<String> maps = new ArrayList<>(Arrays.asList(
	                //"maps/24x24/basesWorkers24x24A.xml",
	                //"maps/32x32/basesWorkers32x32A.xml"
	                //"maps/8x8/basesWorkers8x8A.xml"
	        		//"maps/NoWhereToRun9x8.xml"
	        //"maps/BroodWar/(4)BloodBath.scmB.xml"
	        		//"maps/16x16/basesWorkers16x16A.xml"
	        		//"maps/BroodWar/(4)EmpireoftheSun.scmB.xml"
	        		"maps/battleMaps/Others/RangedHeavyMixed.xml"
	        		
	        		//"maps/8x8/FourBasesWorkers8x8.xml"
	        		//"maps/16x16/TwoBasesBarracks16x16.xml"
	        		//"maps/BWDistantResources32x32.xml"
	        		//"maps/DoubleGame24x24.xml"
	        ));

	        UnitTypeTable utt = new UnitTypeTable();
	        PhysicalGameState pgs = PhysicalGameState.load(maps.get(0), utt);

	        GameState gs = new GameState(pgs, utt);
	        int MAXCYCLES = 1000;
	        int PERIOD = 20;
	        boolean gameover = false;

	        if (pgs.getHeight() == 8) {
	            MAXCYCLES = 2000;
	        }
	        if (pgs.getHeight() == 9) {
	            MAXCYCLES = 3000;
	        }
	        if (pgs.getHeight() == 16) {
	            MAXCYCLES = 3000;
	        }
	        if (pgs.getHeight() == 24) {
	            MAXCYCLES = 4000;
	        }
	        if (pgs.getHeight() == 32) {
	            MAXCYCLES = 12000;
	        }
	        if (pgs.getHeight() == 64) {
	            MAXCYCLES = 17000;
	        }
	        if (pgs.getHeight() == 128) {
	            MAXCYCLES = 17000;
	        }

	        //decompõe a tupla
//	        ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
//	        String[] itens = tupleAi1.replace("[", "").replace("]", "").split(";");
//
//	        for (String element : itens) {
//	            iScriptsAi1.add(Integer.decode(element));
//	        }
//
//	        ArrayList<Integer> iScriptsAi2 = new ArrayList<>();
//	        itens = tupleAi2.replace("[", "").replace("]", "").split(";");
//
//	        for (String element : itens) {
//	            iScriptsAi2.add(Integer.decode(element));
//	        }
//
//
//
//	      
//	        List<AI> scriptsRun1=RoundRobinTOScale_GP.decodeScripts(utt, iScriptsAi1);
//	        List<AI> scriptsRun2=RoundRobinTOScale_GP.decodeScripts(utt, iScriptsAi2);
	      	//AI ai1 = new LightPGSSCriptChoiceNoWaits(utt, scriptsRun1,200, "PGSR");
	      	//AI ai2 = new LightPGSSCriptChoiceNoWaits(utt, scriptsRun2,200, "PGSR");
	      	
	      	AI ai1=RoundRobinTOScale_GP.buildCommandsIA(utt, tupleAi1);
	      	AI ai2=RoundRobinTOScale_GP.buildCommandsIA(utt, "attack(Heavy,closest)");
	        
	      
//	      AI ai1 = new CmabAssymetricMCTS(100, -1, 100, 1, 0.3f, 
//	                                           0.0f, 0.4f, 0, new RandomBiasedAI(utt), 
//	                                           new SimpleSqrtEvaluationFunction3(), true, utt, 
//	                                          "ManagerClosestEnemy", 1,decodeScripts(utt, iScriptsAi1));
//	      
//	      AI ai2 = new CmabAssymetricMCTS(100, -1, 100, 1, 0.3f, 
//	                                           0.0f, 0.4f, 0, new RandomBiasedAI(utt), 
//	                                           new SimpleSqrtEvaluationFunction3(), true, utt, 
//	                                          "ManagerClosestEnemy", 1,decodeScripts(utt, iScriptsAi2));
	      
//	      AI ai1 = new GABScriptChoose(utt, 1, 2, decodeScripts(utt, iScriptsAi1), "GAB");
//	      AI ai2 = new GABScriptChoose(utt, 1, 2, decodeScripts(utt, iScriptsAi2), "GAB");
//	      AI ai1 = new SABScriptChoose(utt, 1, 2, decodeScripts(utt, iScriptsAi1), "SAB");
//	      AI ai2 = new SABScriptChoose(utt, 1, 2, decodeScripts(utt, iScriptsAi2), "SAB");

	        /*
	            Variáveis para coleta de tempo
	         */
	        double ai1TempoMin = 9999, ai1TempoMax = -9999;
	        double ai2TempoMin = 9999, ai2TempoMax = -9999;
	        double sumAi1 = 0, sumAi2 = 0;
	        int totalAction = 0;

	        log.add("---------AIs---------");
	        log.add("AI 1 = " + ai1.toString());
	        log.add("AI 2 = " + ai2.toString() + "\n");

	        log.add("---------Mapa---------");
	        log.add("Mapa= " + maps.get(0) + "\n");

	        //método para fazer a troca dos players
	        JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 840, 840, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
//	        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);
	        long startTime;
	        long timeTemp;
	        long countingTimeAI1=0;
	        long countingTimeAI2=0;
	        long counterCallsAI1=0;
	        long counterCallsAI2=0;
	        

	        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
	        do {
	            if (System.currentTimeMillis() >= nextTimeToUpdate) {
	                totalAction++;
	                startTime = System.currentTimeMillis();

	                PlayerAction pa1 = ai1.getAction(0, gs);
	                //dados de tempo ai1
	                timeTemp = (System.currentTimeMillis() - startTime);
	                
	                if(timeTemp>0)
	                {
	                	counterCallsAI1++;
	                	countingTimeAI1=countingTimeAI1+timeTemp;
	                }
	                
	                sumAi1 += timeTemp;
	                //coleto tempo mínimo
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
	                
	                if(timeTemp>0)
	                {
	                	counterCallsAI2++;
	                	countingTimeAI2=countingTimeAI1+timeTemp;
	                }
	                
	                sumAi2 += timeTemp;
	                //coleto tempo mínimo
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
	                w.repaint();
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
	        
	        if(counterCallsAI1>0)
	        	log.add("Avg Response "+ai1.toString()+" "+ countingTimeAI1/counterCallsAI1);
	        if(counterCallsAI2>0)
	        	log.add("Avg Response "+ai2.toString()+" "+ countingTimeAI2/counterCallsAI2);
	        

	        
	        //System.exit(0);

	        
		
	}

	private boolean resetPopulation(Population population2) {
		if (ConfigurationsGA.RESET_ENABLED) {
			if (population2.isPopulationValueZero()) {
				System.out.println("Population reset!");
				return true;
			}
		}
		return false;
	}

	private void updateGeneration() {
		this.generations++;
	}

	private boolean continueProcess() {
		switch (ConfigurationsGA.TYPE_CONTROL) {
		case 0:
			return hasTime();

		case 1:
			return hasGeneration();

		default:
			return false;
		}

	}

	private boolean hasGeneration() {
		if (this.generations < ConfigurationsGA.QTD_GENERATIONS) {
			return true;
		}
		return false;
	}

	/**
	 * Função que inicia o contador de tempo para o critério de parada
	 */
	protected void resetControls() {
		this.timeInicial = Instant.now();
		this.generations = 0;
	}

	protected boolean hasTime() {
		Instant now = Instant.now();

		Duration duracao = Duration.between(timeInicial, now);

		// System.out.println( "Horas " + duracao.toMinutes());

		if (duracao.toHours() < ConfigurationsGA.TIME_GA_EXEC) {
			return true;
		} else {
			return false;
		}

	}
	
	public String recoverScriptGenotype(String portfolioIds)
	{
		String portfolioGenotype;
        ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
        portfolioIds = portfolioIds.replaceAll("\\s+","");
        String[] itens = portfolioIds.replace("[", "").replace("]", "").split(",");

        for (String element : itens) {
            iScriptsAi1.add(Integer.decode(element));
        }
        
        portfolioGenotype=buildScriptsTable(pathTableScripts).get(BigDecimal.valueOf(iScriptsAi1.get(0)));
       
		return portfolioGenotype;
	}
	
    public HashMap<BigDecimal, String> buildScriptsTable(String pathTableScripts) {
    	HashMap<BigDecimal, String> scriptsTable = new HashMap<>();
        String line="";
        try (BufferedReader br = new BufferedReader(new FileReader(pathTableScripts + "ScriptsTable.txt"))) {
            while ((line = br.readLine()) != null) {
                String code = line.substring(line.indexOf(" "), line.length());
                String[] strArray = line.split(" ");
                int idScript = Integer.decode(strArray[0]);
                scriptsTable.put(BigDecimal.valueOf(idScript), code);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block            
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(Exception e){
            System.out.println(line);
            System.out.println(e);
        }

        return scriptsTable;
    }
	
}

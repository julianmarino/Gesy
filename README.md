# Gesy

This repository contains the code of Gesy, a Genetic-Programming Scripts-Synthesizer for microRTS.

The framework is divided into three folders:

MicroRTS - It is the microRTS necessary to execute the game matches required by the Fitness Function of Gesy. The user needs to generate the executable jar setting the main class SOARoundRobinTOScale_GP. 

Genetic Programming Algorithm - It is Gesy. Here, you can change variables such as mutation rate, crossover rate, the number of generations, and other evolutionary parameters. Namely, you can change these parameters in the file ConfiguratiosGA.java. Also, in this file, you can set the parameter  gesyS==false if you want to run the original version of Gesy, and gesyS==true if you want to run Gesy_s, a version of Gesy that includes some scripts developed by professionals in the initial population.

Scripts - IN this folder, you can find the necessary scripts required to run the framework in computational clusters, running several MicroRTS matches parallel. 

We advise using the folder Configured Sample to support the initial tests. It is fully configured. To run that example in any cluster, you need to run the 1runGA.sh and after that 2runSOA.sh. The first script will run the Gesy, and the second one will perform the microRTS clients to process all matches. 

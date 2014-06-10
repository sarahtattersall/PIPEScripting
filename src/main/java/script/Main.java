package script;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.cli.*;
import pipe.reachability.algorithm.*;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.steadystate.algorithm.SteadyStateBuilder;
import pipe.steadystate.algorithm.SteadyStateBuilderImpl;
import pipe.steadystate.algorithm.SteadyStateSolver;
import uk.ac.imperial.io.*;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.io.PetriNetIOImpl;
import uk.ac.imperial.pipe.io.PetriNetReader;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to run state space exploration and optionally steady state solver
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final Options options = new Options();
    static {
        options.addOption("f", true, "File to parse");
        options.addOption("s", false, "Run sequentially");
        options.addOption("p", true, "Run in parallel with the specified number of threads per state");
        options.addOption("ss", false, "Solve the steady state with jacobi");
        options.addOption("b", true, "Bound for Jacobi");
    }

    /**
     *
     * Args should be:
     *  - filename e.g. models/foo.xml
     *  - algorithm p => parallel, s => sequential
     *  - solve steady state too? y => yes n => no
     *  - if processing in parallel number of threads allowed
     *  -
     *
     *
     * @param args
     * @throws JAXBException
     * @throws UnparsableException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws InvalidRateException
     * @throws TimelessTrapException
     * @throws IOException
     */
    public static void main(String[] args)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, InvalidRateException,
            TimelessTrapException, IOException, ParseException {
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse( options, args);
        if (cmd.hasOption("f")) {
            String filename = cmd.getOptionValue("f");
            PetriNet petriNet = readPetriNet(filename);
            if (cmd.hasOption("s")) {
                processSequential(petriNet, cmd);
            } else {
                int states;
                if (cmd.hasOption("p")) {
                    states = Integer.parseInt(cmd.getOptionValue("p"));
                } else {    //Default
                    states = 100;
                }
                processParallel(petriNet, states, cmd);
            }
        } else {
           LOGGER.log(Level.SEVERE, "Need a file name");
        }
    }

    public static PetriNet readPetriNet(String path) throws JAXBException, UnparsableException, IOException {
        PetriNetReader io = new PetriNetIOImpl();
        return io.read(path);
    }

    private static void processParallel(PetriNet petriNet, int statesPerThread, CommandLine cmd)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException, InvalidRateException {

        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor =new StateIOProcessor(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new UnboundedExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = new OnTheFlyVanishingExplorer(explorerUtilities);

                MassiveParallelStateSpaceExplorer stateSpaceExplorer =
                        new MassiveParallelStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor, statesPerThread);

                explore(stateSpaceExplorer, explorerUtilities, " Parallel " + statesPerThread);

            }
            if (cmd.hasOption("ss")) {
                solveSteadyState(kryoIo, transitions, state, cmd);
            }
        }

    }

    private static void solveSteadyState(KryoStateIO kryoIo, Path transitions, Path state, CommandLine cmd) throws IOException {
        try (InputStream transitionInputStream = Files.newInputStream(transitions);
             InputStream stateStream = Files.newInputStream(state);
             Input inputStream = new Input(transitionInputStream);
             Input stateInputStream = new Input(stateStream)) {
            MultiStateReader reader = new EntireStateReader(kryoIo);
            List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
            Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);

            SteadyStateBuilder builder = new SteadyStateBuilderImpl();
            ExecutorService executorService = null;
            SteadyStateSolver solver;
            if (cmd.hasOption("s")) {
                if (cmd.hasOption("b")) {
                    Integer maxIterations = Integer.valueOf(cmd.getOptionValue("b"));
                    solver = builder.buildBoundedSequentialJacobi(maxIterations);
                } else {
                    solver = builder.buildSequentialJacobi();
                }
            } else {
                executorService = Executors.newFixedThreadPool(8);
                if (cmd.hasOption("b")) {
                    Integer maxIterations = Integer.valueOf(cmd.getOptionValue("b"));
                    solver = builder.buildBoundedParallelJacobiSolver(executorService, 8, maxIterations);
                } else {
                    solver = builder.buildJacobi(executorService, 8);
                }
            }
            Map<Integer, Double> steadyState = solve(solver, records, "Parallel");
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }
    private static Map<Integer, Double> solve(SteadyStateSolver solver, List<Record> records, String method) {
        return solver.solve(records);
    }

    private static void processSequential(PetriNet petriNet, CommandLine cmd)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException, InvalidRateException {

        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor =new StateIOProcessor(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new UnboundedExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = new OnTheFlyVanishingExplorer(explorerUtilities);

                SequentialStateSpaceExplorer stateSpaceExplorer =
                        new SequentialStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor);

                explore(stateSpaceExplorer, explorerUtilities, " Sequential ");
            }
            if (cmd.hasOption("ss")) {
                solveSteadyState(kryoIo, transitions, state, cmd);
            }
        }
    }

    private static void explore(StateSpaceExplorer explorer,  ExplorerUtilities explorerUtilities, String name)
            throws InterruptedException, ExecutionException, IOException, TimelessTrapException, InvalidRateException {
        System.out.println("Starting " + name);
        System.out.println("========================");
        StateSpaceExplorer.StateSpaceExplorerResults result = explorer.generate(explorerUtilities.getCurrentState());

        System.out.println("Processed transitions: " + result.processedTransitions);
        System.out.println(result.numberOfStates + " different states");
    }


}

package script;

import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.algorithm.*;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.StateIOProcessor;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.io.PetriNetIOImpl;
import uk.ac.imperial.pipe.io.PetriNetReader;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, InvalidRateException,
            TimelessTrapException, IOException {
        String file = args[0];
        String algo = args[1];

        PetriNet petriNet = readPetriNet(file);
        if (algo.equals("s")) {
            processSequential(petriNet);
        } else {
            int states = Integer.parseInt(args[2]);
            processParallel(petriNet, states);
        }




    }

    public static PetriNet readPetriNet(String path) throws JAXBException, UnparsableException, IOException {
        PetriNetReader io = new PetriNetIOImpl();
        return io.read(path);
    }

    private static void processParallel(PetriNet petriNet, int statesPerThread)
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
            //            try (InputStream transitionInputStream = Files.newInputStream(transitions);
            //                 InputStream stateStream = Files.newInputStream(state);
            //                 Input inputStream = new Input(transitionInputStream);
            //                 Input stateInputStream = new Input(stateStream)) {
            //                MultiStateReader reader = new EntireStateReader(kryoIo);
            //                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
            //                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
            //
            //                SteadyStateBuilder builder = new SteadyStateBuilderImpl();
            //                ParallelSteadyStateSolver solver = new ParallelSteadyStateSolver(8, builder);
            //                Map<Integer, Double> steadyState = solve(solver, records, "Parallel");
            //                System.out.println("----------------------");
            ////                GaussSeidelSolver gaussSeidelSolver = new GaussSeidelSolver();
            ////                solve(gaussSeidelSolver, records, "Gauss Seidel");
            //
            //
            //            }
        }

    }
    private static void processSequential(PetriNet petriNet)
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
            //            try (InputStream transitionInputStream = Files.newInputStream(transitions);
            //                 InputStream stateStream = Files.newInputStream(state);
            //                 Input inputStream = new Input(transitionInputStream);
            //                 Input stateInputStream = new Input(stateStream)) {
            //                MultiStateReader reader = new EntireStateReader(kryoIo);
            //                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
            //                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
            //
            //
            //                GaussSeidelSolver solver = new GaussSeidelSolver();
            ////                Map<Integer, Double> steadyState = solve(solver, records, "Gauss Seidel");
            //
            //
            //            }
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

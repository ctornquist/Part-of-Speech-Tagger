import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;


/**
 * Program to tag the part of speech (POS) of words using Hidden Markov Models and Viterbi decoding.
 *
 * @author Caroline Tornquist, Fall 2019 updated June 2020
 */

public class Speech {
    ArrayList<String> training = new ArrayList<>();                      //list of training words
    ArrayList<String> trainingTags = new ArrayList<>();                  //list of training tags associated with above
    ArrayList<String> forHash = new ArrayList<>();                       //helping w/ the starts of sentences
    BufferedReader input;
    Map<String, Map<String,Double>> transition = new HashMap<>();        //maps each POS to every other POS and the prob. that the second follows the first
    Map<String, Map<String, Integer>> numFollows = new HashMap<>();      //maps each POS to every other POS and the # of times the second follows the first
    Map<String, Integer> numPOSAppears = new HashMap<>();                //maps each POS to the # of times it appears
    Map<String, Integer> numWordAppears = new HashMap<>();               //maps each word to the # of time it appears
    Map<String, Map<String, Double>> observation = new HashMap<>();      //maps each POS to the words that appear as that POS and their "observation score"
    String trainingSentences = "brown-train-sentences.txt";              //file with sentencse to test on
    String trainingSentenceTags = "brown-train-tags.txt";                //file with matched POS to trainingSentences
    String testingSentences = "brown-test-sentences.txt";                //file with test sentences
    String testingSentencesTags = "brown-test-tags.txt";                 //file with matched POS to testingSentences to calculate correctness

    /*
     * Right now is set to "console" which lets the user type in sentences. Comment out the console line and
     * uncomment "tester" to test the program on 36,000 words (takes ~1 min to run).
     */
    public Speech(){
        readInTrainings();
        makeNumPOSAppears();
        makeNumFollows();
        makeTransitionMap();
        makeNumWordAppears();
        makeObservationMap();

        //tester();
        console();
    }

    /**
     * Opens all of the training files and reads each word into the word or POS arrays
     */
    public void readInTrainings () {
        System.out.println("Training the model on 35000 words...");

        try {                                                       //opening sentences file
            input = new BufferedReader(new FileReader(trainingSentences));
        }
        catch (Exception e) {
            System.err.println(e);
        }

        String all = " ";
        try {
            while ((all = input.readLine()) != null) {
                String[] splitUp = all.split(" ");             //splitting based on spaces

                for (String word: splitUp) {
                    word.toLowerCase();                               //making everything lowercase
                    training.add(word);
                }
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }

        try {
            input.close();
        }
        catch (Exception e) {
            System.err.println(e);
        }


        //opening tagged text file
        try {
            input = new BufferedReader(new FileReader(trainingSentenceTags));
        }
        catch (Exception e) {
            System.err.println(e);
        }

        //reading all the tags into trainingTags array
        String all2 = " ";
        try {
            while ((all2 = input.readLine()) != null) {
                String hash = "# "+all2;                               //making array that has hash at the beginning of each sentence
                forHash.add(hash);

                String[] splitUp = all2.split(" ");             //splitting on spaces

                for (String word: splitUp) {
                    word.toLowerCase();                                //making all lowercase
                    trainingTags.add(word);
                }
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }

        //closing input
        try {
            input.close();
        }
        catch (Exception e) {
            System.err.println(e);
        }

    }


    /**
     * Makes a map from each POS to the number of time it appears
     */
    public void makeNumPOSAppears() {
        for(String s: trainingTags) {                           //if it's already in the map, add one
            if (numPOSAppears.containsKey(s)) {
                int val = numPOSAppears.get(s);
                numPOSAppears.put(s, val+1);
            }
            else {                                              //else, insert w/ count 1
                numPOSAppears.put(s,1);
            }
        }

        //adding hash to the numPOSAppears map
        numPOSAppears.put("#", forHash.size());
    }


    /**
     * Makes a map from each POS to every other POS and the # of times that POS follows the first one
     */
    public void makeNumFollows () {
        //making a copy of the training tag aray to work with
        ArrayList<String> copyTraining = new ArrayList<>();
        copyTraining.addAll(trainingTags);

        //for every part of speech in the training tags
        for(String pos: trainingTags) {
            int idx = copyTraining.indexOf(pos);                            //getting the index of the POS

            if (copyTraining.size() > 1) {                                    //if we're not at the end of the array
                if (!numFollows.containsKey(pos)) {                         //if that pos isn't in the map, add it and what follows it with count 1
                    numFollows.put(pos, new HashMap<>());
                    numFollows.get(pos).put(copyTraining.get(idx + 1), 1);
                }
                //if that pos exists, but what follows it doesn't, add the follower with count 1
                else if (numFollows.containsKey(pos) && !numFollows.get(pos).containsKey(copyTraining.get(idx + 1))) {
                    numFollows.get(pos).put(copyTraining.get(idx + 1), 1);
                }
                //if the pos and follower exist, add one to the count
                else if (numFollows.containsKey(pos) && numFollows.get(pos).containsKey(copyTraining.get(idx + 1))) {
                    int val = numFollows.get(pos).get(copyTraining.get(idx + 1));
                    numFollows.get(pos).put(copyTraining.get(idx + 1), val + 1);
                }
            }

            copyTraining.remove(idx);                                       //remove it from the copy so we don't repeat
        }


        //adding hash to the numFollows map
        numFollows.put("#", new HashMap<>());
        for(String sentence: forHash){
            String[] tags = sentence.split(" ");

            if (!numFollows.get("#").containsKey(tags[1])) {
                numFollows.get("#").put(tags[1], 1);
            }
            else {
                int val = numFollows.get("#").get(tags[1]);
                numFollows.get("#").put(tags[1], val+1);
            }
        }

        numFollows.replace(".", new HashMap<>());                           //removing anything following a period
    }


    /**
     * Maps each part of speech to every other part of speech and the probability that the second follows the first.
     * This value is the "transition value" and is the ln(#times it follows/# time appears)
     */
    public void makeTransitionMap () {
        for(String pos: numFollows.keySet()) {
            transition.put(pos, new HashMap<>());                               //add that part of speech to the transition map

            for(String follower: numFollows.get(pos).keySet()){
                double followed = numFollows.get(pos).get(follower);            //the number of times follower followed pos
                double appeared = numPOSAppears.get(pos);                       //the number of times that pos appeared
                transition.get(pos).put(follower, Math.log(followed/appeared));
            }
        }
    }


    /**
     * Maps each word to the number of times it appears in the training file
     */
    public void makeNumWordAppears(){
        for(String s: training) {
            if (numWordAppears.containsKey(s)) {            //if it's already in the map, add one
                int val = numWordAppears.get(s);
                numWordAppears.put(s, val+1);
            }
            else {
                numWordAppears.put(s,1);                    //add new elt with frequency 1
            }
        }
    }


    /**
     * Makes the (nested) observation map. Maps each POS to every word of that type and the
     * ln(frequency of the word/number of times that POS appears). This is the "observation score."
     */
    public void makeObservationMap () {
        //making copies of the arrays so can remove things from them
        ArrayList<String> copyTraining = new ArrayList<>();
        copyTraining.addAll(training);
        ArrayList<String> copyTags = new ArrayList<>();
        copyTags.addAll(trainingTags);

        //Preliminary step of creating a map that hold the number of observations for each word in each POS
        //ex "present" could be a noun or a verb; this helps us distinguish the two
        for (String pos: trainingTags) {                                //for each tagger
            int idx = copyTags.indexOf(pos);                            //get the index of that tagger

            //if the POS isn't in the map yet, add it w/ that word and count 1
            if (!observation.containsKey(pos)) {
                observation.put(pos, new HashMap<>());
                observation.get(pos).put(copyTraining.get(idx), 1.0);   //add count as 1
            }
            //if the POS is in the map, but the word isn't, add the word with count 1
            else if (observation.containsKey(pos) && !observation.get(pos).containsKey(copyTraining.get(idx))) {
                observation.get(pos).put(copyTraining.get(idx), 1.0);   //add count as 1
            }
            //if the POS and word are the map, add one to the count
            else if (observation.containsKey(pos) && observation.get(pos).containsKey(copyTraining.get(idx))) {
                double val = observation.get(pos).get(copyTraining.get(idx));
                observation.get(pos).put(copyTraining.get(idx), val+1);  //update the count

            }

            //remove those keys from the copies so we don't get them again
            copyTags.remove(idx);
            copyTraining.remove(idx);
        }



        //Making the actual observation score and updating the map with those numbers.
        //The observation score is the ln(frequency of the word/number of times that pos appears).
        for(String pos: observation.keySet()){
            for(String word: observation.get(pos).keySet()){
                double obsScore = Math.log(observation.get(pos).get(word)/numPOSAppears.get(pos));
                observation.get(pos).put(word, obsScore);
            }
        }

        //adding hash to the observation
        observation.put("#", new HashMap<>());
        observation.get("#").put("#", 0.0);

    }

    /**
     * Using viterbi decoding to determine the parts of speech of a sentence passed into the method.
     */
    public List<String> viterbi (String sentence) {
        //making all the maps
        Set<String> currStates = new HashSet<>();                               //the current possible parts of speech
        Map<String,Double> currScores = new HashMap<>();                        //the current scores for each possible POS
        ArrayList<Map<String, String>> backtrack = new ArrayList<>();           //all possible "routes" of parts of speech for the sentence
        ArrayList<String> viterbiTags = new ArrayList<>();                      //final tags
        int penalty = -100;

        //putting starting values in
        sentence.toLowerCase();
        String[] obs = sentence.split(" ");
        currStates.add("#");
        currScores.put("#", 0.0);


        for (int i=0; i<obs.length; i++) {                                      //for every word in the sentence
            Set<String> nextStates = new HashSet<>();                           //the next possible parts of speech
            Map<String, Double> nextScores = new HashMap<>();                   //the next possible scores

            for (String pos : currStates) {                                     //for each pos in currStates
                for (String fpos : transition.get(pos).keySet()) {               //for each follower of that pos
                    nextStates.add(fpos);

                    double nextScore = currScores.get(pos) + transition.get(pos).get(fpos); //getting the score

                    if (observation.get(fpos).containsKey(obs[i])) {             //if it's observed, get the obs score
                        nextScore += observation.get(fpos).get(obs[i]);
                    } else {                                                     //else add the penalty
                        nextScore += penalty;
                    }

                    //if that follower isn't there yet or the score is better
                    if (!nextScores.containsKey(fpos) || nextScore > nextScores.get(fpos)) {
                        nextScores.put(fpos, nextScore);

                        //add to backtrack
                        if (backtrack.size() < i + 1) {
                            backtrack.add(new HashMap<>());
                            backtrack.get(i).put(fpos, pos);
                        } else {
                            backtrack.get(i).put(fpos, pos);
                        }
                    }
                }
            }

            currStates = nextStates;                                            //update to next word
            currScores = nextScores;
        }


        //start at the last word, then backtrack to get the most likely tag
        String state = " ";
        double bestScore = -1000000000;

        for(String pos: currStates){                                            //finding the best tag for the last word
            if (currScores.get(pos) > bestScore) {
                bestScore = currScores.get(pos);
                state = pos;
            }
        }
        viterbiTags.add(state);                                                 //adding that pos to tags
        for(int i = obs.length -1; i>0; i--) {                                  //working backwards
            String newState = backtrack.get(i).get(state);
            viterbiTags.add(0, newState);
            state = newState;
        }
        return viterbiTags;
    }


    /**
     * Creates a console based system to allow the user to input a sentence and tell them what eac part of speech is
     */
    public void console () {
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("\n Please type your sentence: ");
            String sentence = in.nextLine();
            sentence = sentence.toLowerCase();
            List<String> tags = viterbi(sentence);

            String[] splitUp = sentence.split(" ");
            System.out.println(" ");
            System.out.println("The parts of speech for the sentence '" +sentence+ "' are: ");
            for(int i=0; i<tags.size(); i++){
                System.out.println(splitUp[i]+" = "+tags.get(i));
            }
        }
    }

    /**
     * Passes sentences from a test file into the viterbi method and then calculates the accuracy.
     */
    public void tester (){
        List<String> words = new ArrayList<>();                             //sentences read ing
        List<String> myTags = new ArrayList<>();                            //what I tagged them as
        List<String> realTags = new ArrayList<>();                          //what the actual tag should be

        try {
            input = new BufferedReader(new FileReader(testingSentences));
        }
        catch (Exception e) {
            System.err.println(e);
        }

        String sentence;
        try {
            while ((sentence = input.readLine()) != null) {
                myTags.addAll(viterbi(sentence));

                String[] splitUp = sentence.split(" ");

                for (String s : splitUp) {
                    words.add(s);
                }
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }

        try{
            input.close();
        }
        catch(Exception e) {
            System.err.println(e);
        }

        try {
            input = new BufferedReader(new FileReader(testingSentencesTags));
        }
        catch (Exception e){
            System.err.println(e);
        }

        String theTags;
        try {
            while ((theTags = input.readLine()) != null) {
                String[] splitUp = theTags.split(" ");

                for (String s : splitUp) {
                    realTags.add(s);
                }
            }
        }
        catch (Exception e) {
            System.err.println(e);
        }

        try{
            input.close();
        }
        catch(Exception e){
            System.err.println(e);
        }


        int totalCorrect = 0;
        int total = 0;
        for (String test: myTags){
            if (test.compareTo(realTags.get(total)) == 0){
                totalCorrect += 1;
            }
            total += 1;
        }

        double score = (double)totalCorrect/total;
        System.out.printf("Got %d/%d correct. Score is %.1f%c. That's an A!", totalCorrect, total, score*100, '%');
    }

    public static void main(String[] args) {
        Speech predictor = new Speech();

    }


}

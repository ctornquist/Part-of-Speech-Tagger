# Part-of-Speech-Tagger
Uses Hidden Markov Models and Viterbi decoding to tell you the part of speech of words in a given sentence. 

### To Run
Right now is set on "console" which allows the user to type in a sentence and the program will return the part of speech for each word. To test it on a data set of ~36,000 words uncomment the "tester()" line in the constructor.


### Overview
      This program uses Hidden Markov Models and Viterbi decoding to tag the parts of speech in sentences. This is challenging because one word could be  different parts of speech in different contexts. For example, "present" could be a verb ("I'd like to present you with this opportunity"); an adjective ("it'll be easier if you're present the meeting"); or a noun ("no time like the present"). The model calculates the probability each POS is followed by each other POS, the "transition score," as well as the probability each word showed up as a particlar POS, the "observation score."   
      The decoder works by calculating the probability of each POS for each word and storing them in a map. Then, it determines the most probable POS for the last word, then the most likely POS that preceded it and so on until you reach the beginning of the sentence.  
      With a large data set, provided by Brown University, I was able to create a model that had 96% accuracy on a test set of almost 36,000 words. However, training the model is fairily slow (~40 sec) so I'm still working on making it more efficient.   


### Tags
Key to help decipher the abbreviations used in the program. 

| ABV | meaning |
|-------|---------|
| ADJ | adjective |
| ADV | adverb |
| CNJ | conjunction |
| DET | determinatnt |
| EX | existential |
| FW | foreign word |
| MOD | modular verb |
| N | noun |
| NP | proper noun |
| NUM | number |
| PRO | pronoun |
| P | preppsition |
| TO | the word "to" |
| UH | interjection |
| V | verb |
| VD | past tense verb |
| VG | present participle |
| VN | past participle |
| WH | wh determiner | 

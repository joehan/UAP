package edu.cmu.ark;

import java.io.*;
import java.util.*;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.GlobalProperties;
import edu.cmu.ark.InitialTransformationStep;
import edu.cmu.ark.Question;
import edu.cmu.ark.QuestionRanker;
import edu.cmu.ark.QuestionTransducer;
import edu.stanford.nlp.trees.Tree;

public class Runner {
	private static QuestionTransducer qt;
	
	public static void generateQuestions(String articlePath) throws IOException, ClassNotFoundException{
		qt = new QuestionTransducer();
		GlobalProperties.setDebug(true);
		
		String doc = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("corpus/"+File.separator+articlePath)));
		String buf;
		
		while((buf = br.readLine()) != null){
			if(buf.matches("^.*\\S.*$")){
				doc += buf + " ";
			}else{
				doc += "\n";
			}
		}
		
		List<String> sentences = AnalysisUtilities.getSentences(doc);
		
		//iterate over each segmented sentence and generate questions
		List<Tree> inputTrees = new ArrayList<Tree>();
		Tree parsed;
		
		for(String sentence: sentences){
			parsed = AnalysisUtilities.getInstance().parseSentence(sentence).parse;
			inputTrees.add(parsed);
		}
		
		//step 1 transformations
		InitialTransformationStep trans = new InitialTransformationStep();
		List<Question> transformationOutput = trans.transform(inputTrees);
		
		//step 2 question transducer
		List<Question> outputQuestionList = new ArrayList<Question>();
		for(Question t: transformationOutput){
			qt.generateQuestionsFromParse(t);
			outputQuestionList .addAll(qt.getQuestions());
		}			
		
		//remove duplicates
		QuestionTransducer.removeDuplicateQuestions(outputQuestionList);
		
		//step 3 ranking
		String modelPath = "models" + File.separator + "linear-regression-ranker-reg500.ser.gz";
		QuestionRanker qr = new QuestionRanker();
		qr.loadModel(modelPath);
		qr.scoreGivenQuestions(outputQuestionList);
		boolean doStemming = true;
		QuestionRanker.adjustScores(outputQuestionList, inputTrees, true, true, true, doStemming);
		QuestionRanker.sortQuestions(outputQuestionList, false);
		
		
		//now print the questions
		String name = articlePath.substring(0,articlePath.length()-4);
		PrintWriter writer = new PrintWriter("/home/joe/workspace/QuestionGen/corpus/Questions/"+name+"Questions.txt", "UTF-8");
		System.out.println(name+"Questions.txt");
		
		for(Question question: outputQuestionList){
			String out = AnalysisUtilities.getCleanedUpYield(question.getTree())+"\t"+question.getScore();
			System.err.println(out);
			writer.println(out);
		}
		writer.close();
		qt = null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException{
//		File folder = new File("/home/joe/workspace/QuestionGen/corpus/Transcripts/");
//		//System.out.print(folder);
//		File[] files = folder.listFiles();
//		//System.out.print(files);
//		for (File file:files){
//			String fileName = file.getName();
			String fileName = "OCWlectureNotes2.txt";
			try{
				generateQuestions(fileName);
			} catch(Exception e){
				System.out.println(e.toString());
			}
//		}
		
	}
}

import java.io.*;
import java.util.regex.*;

/**
 * Code by Jay Dance
 */

public class WhereInTheWorld {
    public static void main(String[] args) {
        formatInput();
    }
    
    /**
     * A method that takes a .txt file as input, read through and formats each line 
     * then adds the formatted line to a .txt output file
     */
    public static void formatInput() {
        String inputFile = "input.txt";      //Name of the input file
        String outputFile = "formattedInput.txt";    //Desired name of output file

        try {
            FileReader fileReader = new FileReader(inputFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(outputFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            String line;
            boolean firstLine = true; // Keep track of if it is the first line

            while ((line = bufferedReader.readLine()) != null) {
                String formattedLine = formatLine(line);
                if (!formattedLine.isEmpty()) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        bufferedWriter.newLine(); // Add a new line only if it's not the first line
                    }
                    bufferedWriter.write(formattedLine);
                }
            }
            bufferedWriter.close();
            bufferedReader.close();
            fileWriter.close();
            fileReader.close();

            System.out.println("File formatting completed!");

            geojsonCreator();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method that takes a line of input and formats it into lat/long values and an optional location name 
     */
    public static String formatLine(String line) {
        // Define regex pattern and matcher
        Pattern decimalPattern = Pattern.compile("(-?\\d+(\\.\\d+)?)");
    
        Matcher decimalMatcher = decimalPattern.matcher(line);
    
        double latitude = 0.0;
        double longitude = 0.0;
        double latitudeSign = 1.0;
        double longitudeSign = 1.0;

        //Break the input into parts for later checks/processing
        String[] parts = line.split(" ");

        //Check if there is a S or W indicator and if so make lat/long value negative
        for(String part : parts){
            if (part.equals("S") || part.equals("S,")) {
                latitudeSign = -1.0;
            }
            if (part.equals("W") || part.equals("W,")) {
                longitudeSign = -1.0;
            }
        }
        // Count amount of numbers found in the line, helps determine what form the input is in
        int numberCount = 0; 
        Pattern countPattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Matcher countMatcher = countPattern.matcher(line);

        while (countMatcher.find()) {
            numberCount++;
        }

        if (numberCount == 6) {
            line = dmsHandler(line);    
        }else if(numberCount == 4){
            line = dmHandler(line);
        }else if (numberCount == 2) {
            decimalMatcher.find();
            latitude = Double.parseDouble(decimalMatcher.group());
            decimalMatcher.find();
            longitude = Double.parseDouble(decimalMatcher.group());
            if(validLatLong(Double.toString(latitude), Double.toString(longitude))){
                System.err.println("Unable to process: " + line);
                return "";
            }
            //Make values negative if required
            line = (latitude * latitudeSign) + " " + (longitude * longitudeSign);
        }else{
            System.err.println("Unable to process: " + line);
            return "";
        }

        //Check if a location name is included in the line
        boolean locationName = false;
        String location = "";
        boolean containsOnlyLetters = true;
        int words = 1;
        while(parts[parts.length-words].length() > 1 && containsOnlyLetters){
            containsOnlyLetters = parts[parts.length-words].matches("[a-zA-Z]+");
            if(containsOnlyLetters){
                location = parts[parts.length-words] + " " + location;
                locationName = true;
                words++;
            }
        }  
        //If a name was included, add it to the output line
        if(locationName){
            line = line + " " + location;
        }
        System.out.println(line);
        return line;
    }

    /**
     * A method that takes a line in a Decimal Minutes Seconds form and outputs the lat/long values
     */
    public static String dmsHandler(String line) {
        // Split the input
        String[] splitLine = line.split(" ");
    
        // Extract components
        double latitudeDegrees = parseCoord(splitLine[0]);
        double latitudeMinutes = parseCoord(splitLine[1]);
        double latitudeSeconds = parseCoord(splitLine[2].replace("\"", ""));
        double longitudeDegrees = parseCoord(splitLine[4]);
        double longitudeMinutes = parseCoord(splitLine[5]);
        double longitudeSeconds = parseCoord(splitLine[6].replace("\"", ""));
    
        // Determine hemispheres
        String latitudeHemisphere = splitLine[3].toUpperCase();
        latitudeHemisphere = latitudeHemisphere.replaceAll("[^NSEW]", "");
        double latitudeSign = latitudeHemisphere.equals("S") ? -1.0 : 1.0;
        String longitudeHemisphere = splitLine[7].toUpperCase();
        longitudeHemisphere = longitudeHemisphere.replaceAll("[^NSEW]", "");
        double longitudeSign = longitudeHemisphere.equals("W") ? -1.0 : 1.0;
    
        // Calculate lat/long values
        double latitude = latitudeSign * (latitudeDegrees + latitudeMinutes / 60.0 + latitudeSeconds / 3600.0);
        double longitude = longitudeSign * (longitudeDegrees + longitudeMinutes / 60.0 + longitudeSeconds / 3600.0);
        //Format to 6 decimal places
        String roundedlat = String.format("%.6f", latitude);
        String roundedlong = String.format("%.6f", longitude);
        //Build output string
        line = roundedlat + " " + roundedlong;
        //Check lat/long values are valid
        if(validLatLong(roundedlat, roundedlong)){
            System.err.println("Unable to process: " + line);
            return "";
        }
        return line;
    }

    /**
     * A helper method that turns a String into only numbers or '.+-' chars and then returns the value as a double
     */
    public static double parseCoord(String component) {
        // Extract the numeric part of the DMS component
        String numbersOnly = component.replaceAll("[^0-9.+-]", "");

        if (numbersOnly.isEmpty()) {
            return 0.0;
        }
        // Parse the numeric part as a double
        return Double.parseDouble(numbersOnly);
    }

    /**
     * A method that takes a line in a Decimal Minutes form and outputs the lat/long values
     */
    public static String dmHandler(String line) {
        // Split the input
        String[] splitLine = line.split(" ");
    
        // Extract components
        double latitudeDegrees = parseCoord(splitLine[0]);
        double latitudeMinutes = parseCoord(splitLine[1].replace("'", ""));
        double longitudeDegrees = parseCoord(splitLine[3]);
        double longitudeMinutes = parseCoord(splitLine[4].replace("'", ""));
    
        // Determine hemispheres
        String latitudeHemisphere = splitLine[2].toUpperCase();
        latitudeHemisphere = latitudeHemisphere.replaceAll("[^NSEW]", "");
        double latitudeSign = latitudeHemisphere.equals("S") ? -1.0 : 1.0;
        String longitudeHemisphere = splitLine[5].toUpperCase();
        longitudeHemisphere = longitudeHemisphere.replaceAll("[^NSEW]", "");
        double longitudeSign = longitudeHemisphere.equals("W") ? -1.0 : 1.0;
    
        // Calculate lat/long values
        double latitude = latitudeSign * (latitudeDegrees + latitudeMinutes / 60.0);
        double longitude = longitudeSign * (longitudeDegrees + longitudeMinutes / 60.0);

        String roundedlat = String.format("%.6f", latitude);
        String roundedlong = String.format("%.6f", longitude);
        //Build output string
        line = roundedlat + " " + roundedlong;
        //Check lat/long values are valid
        if(validLatLong(roundedlat, roundedlong)){
            System.err.println("Unable to process: " + line);
            return "";
        }
        return line;
    }

    /**
     * A checker method to ensure that the lat/long values are within valid ranges
     */
    public static boolean validLatLong(String sLat, String sLon){
        double lat = Double.parseDouble(sLat);
        double lon = Double.parseDouble(sLon);
        //Check lat/long values are valid
        if(lat < -90 || lat > 90 || lon < -180 || lon > 180){
            return true;
        } else{
            return false;
        }
    }

    /**
     * A method that creates a .geojson file from a .txt file
     */
    public static void geojsonCreator() {
        //Define input/output files
        String inputFile = "formattedInput.txt";
        String outputFile = "output.geojson";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            StringBuilder geojson = new StringBuilder();
            geojson.append("{ \"type\": \"FeatureCollection\", \"features\": [");

            while ((line = reader.readLine()) != null) {
                String[] details = line.trim().split(" ");
                double latitude = Double.parseDouble(details[0]);
                double longitude = Double.parseDouble(details[1]);
                String name = getName(details);
                String feature = String.format("\n{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", " +
                        "\"coordinates\": [ %.6f, %.6f ] }, \"properties\": { \"name\": \"%s\" } },",
                        longitude, latitude, name);
                geojson.append(feature);
            }

            // Remove the trailing comma
            geojson.deleteCharAt(geojson.length() - 1);
            geojson.append("] }");

            writer.write(geojson.toString());
            System.out.println("GeoJSON file created successfully: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error reading or writing file: " + e.getMessage());
        }
    }

    /**
     * A helper method for putting a location name together when creating a GeoJSON file
     */
    public static String getName(String[] nameParts) {
        if (nameParts.length > 2) {
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 2; i < nameParts.length; i++) {
                nameBuilder.append(nameParts[i]);
                if (i < nameParts.length - 1) {
                    nameBuilder.append(" ");
                }
            }
            return nameBuilder.toString();
        }
        return "";
    }
}
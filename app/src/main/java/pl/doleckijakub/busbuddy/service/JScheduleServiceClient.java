package pl.doleckijakub.busbuddy.service;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import pl.doleckijakub.busbuddy.model.City;
import pl.doleckijakub.busbuddy.model.Departure;

public class JScheduleServiceClient {

    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    private static int age = 60;

    private static Document stringToXML(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            return builder.parse(inputSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateAge(City city) {
        try {
            age = 60;
            age = Integer.parseInt(xPath.compile("int").evaluate(stringToXML(getStringImpl(city, "/PingService"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getStringImpl(City city, String path) throws Exception {
        URL url = new URL(city.getScheduleServiceURL() + path);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");

        int age_d = 0; for (char c : city.getCode().toCharArray()) age_d += c;

        urlConnection.setRequestProperty("User-Agent", "myBusOnline");
        urlConnection.setRequestProperty("Age", "" + (age + age_d));
        urlConnection.setUseCaches(false);
        urlConnection.connect();

        InputStream inputStream = urlConnection.getInputStream();
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.next();

        inputStream.close();
        urlConnection.disconnect();

        return result;
    }

    public static String getString(City city, String path) {
        try {
            updateAge(city);
            return getStringImpl(city, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Departure> getDepartures(City city, int stopId) {
        try {
            String s = getString(city, "/GetTimeTableReal?nBusStopId=" + stopId);
            NodeList departures = ((Node) xPath.compile("Departures").evaluate(stringToXML(s), XPathConstants.NODE)).getChildNodes();

            List<Departure> result = new ArrayList<>();

            for (int i = 0; i < departures.getLength(); i++) {
                Node node = departures.item(i);
                if (node.getNodeName().equalsIgnoreCase("D")) {
                    Function<String, String> getNamedString = name -> node.getAttributes().getNamedItem(name).getNodeValue();
                    result.add(new Departure(
                            Integer.parseInt(getNamedString.apply("i")),
                            getNamedString.apply("r"),
                            getNamedString.apply("v"),
                            getNamedString.apply("d"),
                            getNamedString.apply("dd"),
                            getNamedString.apply("t"),
                            getNamedString.apply("n"),
                            getNamedString.apply("p"),
                            getNamedString.apply("m"),
                            getNamedString.apply("vn"),
                            getNamedString.apply("n")
                    ));
                }
            }

            return result;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

}
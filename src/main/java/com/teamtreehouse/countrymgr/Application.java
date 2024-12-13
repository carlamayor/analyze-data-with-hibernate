package com.teamtreehouse.countrymgr;

import com.teamtreehouse.countrymgr.model.Country;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Application {

    //Hold a reusable reference to a SessionFactory
    private static final SessionFactory sessionFactory = buildSessionFactory();
    //for menu input
    private static final BufferedReader readerBuffer = new BufferedReader(new InputStreamReader(System.in));
    //Menu options
    private static final Map<String, String> menuOptions = new LinkedHashMap<>();

    //creates menu for the user in the conosole
    static {
        menuOptions.put("view", "View Countries data.");
        menuOptions.put("statistics", "View Internet users and literacy rate's statistics.");
        menuOptions.put("edit", "Edit country's information.");
        menuOptions.put("add", "Add a new country.");
        menuOptions.put("delete", "Delete a country.");
        menuOptions.put("quit", "Exit the program");
    }

    //Builds Hibernate Session Factory
    private static SessionFactory buildSessionFactory() {
        //Create a StandardServiceRegistry
        final ServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()//loads configuration from hibernate.cfg.xml
                .build();
        try {
            //Builds the SessionFactory using MetadataSources
            return new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (Exception ex) { //Logs exception
            StandardServiceRegistryBuilder.destroy(registry); //destroys the registry
            throw new ExceptionInInitializerError("SessionFactory creation failed: " + ex.getMessage());
        }

    }

    public static void main(String[] args) {
        run();
    }


    //Create the display of the options for the Map menu.
    private static String promptAction() throws IOException {
        System.out.printf("%nWelcome. Your menu options are: %n%n");
        for (Map.Entry<String, String> option : menuOptions.entrySet()) {
            System.out.printf("%-10s - %s %n",
                    option.getKey(),
                    option.getValue());
        }

        System.out.printf("%n%nWhat would you like to do:  %n");
        String choice = readerBuffer.readLine();
        return choice.trim().toLowerCase();
    }

    //Fetch all countries from the database
    private static List<Country> fetchAllCountries() {
        // Open a session
        Session session = sessionFactory.openSession();

        // Create CriteriaBuilder
        CriteriaBuilder builder = session.getCriteriaBuilder();

        // Create CriteriaQuery
        CriteriaQuery<Country> criteria = builder.createQuery(Country.class);

        // Specify criteria root
        criteria.from(Country.class);

        // Execute query
        List<Country> contacts = session.createQuery(criteria).getResultList();

        // Close the session
        session.close();

        return contacts;
    }

    //Shows data in a formatted table
    public static void displayCountries(List<Country> countries) {
        System.out.println();
        System.out.println("COUNTRIES DATA");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("%-5s %30s %25s %20s %n", "Code", "Name", "Internet Users", "Literacy Rate");
        System.out.println("---------------------------------------------------------------------------------------");

        for (Country country : countries) {
            System.out.printf(
                    "%-5s %30s %25s %20s\n",
                    country.getCode(),
                    country.getName(),
                    formattedDecimal(country.getInternetUsers()),
                    formattedDecimal(country.getAdultLiteracyRate())
            );
        }
    }

    //If there is a value we format to two decimals and place NULL if there is nothing.
    private static String formattedDecimal(Double value) {
        if (value == null) {
            return "--";
        }
        return String.format("%.2f", value);
    }

    //Calculate the max and min of internet users and literacy rate to display the statistics after
    public static void displayStatistics(List<Country> countries) {
        Country maxInternetUsers = countries.stream()
                .filter(country -> country.getInternetUsers() != null)
                .max(Comparator.comparing(Country::getInternetUsers))
                .orElse(null);

        Country minInternetUsers = countries.stream()
                .filter(country -> country.getInternetUsers() != null)
                .min(Comparator.comparing(Country::getInternetUsers))
                .orElse(null);

        Country maxLiteracyRate = countries.stream()
                .filter(country -> country.getAdultLiteracyRate() != null)
                .max(Comparator.comparing(Country::getAdultLiteracyRate))
                .orElse(null);

        Country minLiteracyRate = countries.stream()
                .filter(country -> country.getAdultLiteracyRate() != null)
                .min(Comparator.comparing(Country::getAdultLiteracyRate))
                .orElse(null);

        double averageInternetUsers = countries.stream()
                .filter(country -> country.getInternetUsers() != null)
                .mapToDouble(Country::getInternetUsers)
                .average()
                .orElse(0.0);

        double averageLiteracyRate = countries.stream()
                .filter(country -> country.getAdultLiteracyRate() != null)
                .mapToDouble(Country::getAdultLiteracyRate)
                .average()
                .orElse(0.0);

        System.out.println("\n Statistics:");

        System.out.println("--------------------------------------------------------------------");
        System.out.printf("Average Internet users : %.2f  %n",
                averageInternetUsers);
        System.out.printf("Maximum internet users : %.2f (%s) %n",
                maxInternetUsers.getInternetUsers(),
                maxInternetUsers.getName());
        System.out.printf("Minimum internet users : %.2f (%s) %n",
                minInternetUsers.getInternetUsers(),
                minInternetUsers.getName());

        System.out.printf("Average Literacy Rate : %.2f  %n",
                averageLiteracyRate);
        System.out.printf("Maximum Literacy rate : %.2f (%s) %n",
                maxLiteracyRate.getInternetUsers(),
                maxLiteracyRate.getName());
        System.out.printf("Minimum Literacy rate : %.2f (%s) %n",
                minLiteracyRate.getAdultLiteracyRate(),
                minLiteracyRate.getName());
    }

    //Retrieve a contact depending on the code provided
    private static Country fetchCountryByCode(String code) {
        //Open a session
        Session session = sessionFactory.openSession();

        //Retrieve the persistent object (or null if not found)
        Country country = session.get(Country.class, code);

        //Close the session
        session.close();

        //Return the object
        return country;
    }

    //Saves and returns a Country code picked by the user
    private static String countryCode() throws IOException {
        displayCountries(fetchAllCountries());
        System.out.println("Introduce the country code of the country you want to edit: ");
        String code =  readerBuffer.readLine().trim();
        return code;
    }

    //Saves and returns a new Country information to edit country chosen by user
    private static Country countryUpdatedInfo(Country country) throws IOException {
        System.out.println("Introduce the new country name: ");
        String newCountryName = readerBuffer.readLine().trim();
        country.setName(newCountryName);

        System.out.println("Introduce the amount of Internet Users: ");
        Double newInternetUsers = Double.parseDouble(readerBuffer.readLine().trim());
        country.setInternetUsers(newInternetUsers);

        System.out.println("Introduce the amount of literacy rate of the country: ");
        Double newAdultLiteracy = Double.parseDouble(readerBuffer.readLine().trim());
        country.setAdultLiteracyRate(newAdultLiteracy);

        return country;
    }

    //Allows user to edit country data
    private static void editCountry() throws IOException {
        String code = countryCode();
        Country country = fetchCountryByCode(code);
        System.out.println("code is"+country);
        if (country == null) {
            System.out.printf("no country found %s", country);
            return;
        }

        System.out.println("Current country data: ");
        System.out.printf("%nName: %s %n", country.getName());
        System.out.printf("%nInternet Users: %.2f %n", country.getInternetUsers());
        System.out.printf("%nLiteracy Rate: %.2f %n", country.getAdultLiteracyRate());

        System.out.printf("%n%nUpdating...%n%n");
        Session session = sessionFactory.openSession();
        Country newCountry = countryUpdatedInfo(country);
        session.beginTransaction();
        session.merge(newCountry);
        session.getTransaction().commit();
        System.out.println("Country update complete!");
        session.close();
    }

    //Allows user to add country data
    private static void addCountry() throws IOException {
        Session session = sessionFactory.openSession();

        System.out.println("Introduce the new country code: ");
        String code = readerBuffer.readLine().trim().toUpperCase();
        if (code.length() != 3) {
            System.out.println("Invalid code. It has to be 3 characters long.");
            return;
        }
        System.out.println("Introduce the new country name: ");
        String newCountryName = readerBuffer.readLine().trim();
        System.out.println("Introduce the amount of Internet Users: ");
        Double newInternetUsers = Double.parseDouble(readerBuffer.readLine().trim());
        System.out.println("Introduce the amount of literacy rate of the country: ");
        Double newAdultLiteracy = Double.parseDouble(readerBuffer.readLine().trim());

        Country newCountry = new Country.CountryBuilder(code, newCountryName)
                .withInternetUsers(newInternetUsers)
                .withAdultLiteracyRate(newAdultLiteracy)
                .build();
        session.beginTransaction();
        session.save(newCountry);
        session.getTransaction().commit();
        System.out.println("Country added successfully!");
        session.close();
    }

    //Allows user to delete country data
    private static void deleteCountry() throws IOException {
        String code = countryCode();
        Country country = fetchCountryByCode(code);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        System.out.printf("%n%nDeleting...%n%n");
        session.delete(country);
        session.getTransaction().commit();
        System.out.println("Country deleted successfully!");
        session.close();
    }


    public static void run() {

        String choice = "";
        do {
            try {
                choice = promptAction();
                switch (choice) {
                    case "view":
                        displayCountries(fetchAllCountries());
                        break;
                    case "statistics":
                        displayStatistics(fetchAllCountries());
                        break;
                    case "edit":
                        editCountry();
                        break;
                    case "add":
                        addCountry();
                        break;
                    case "delete":
                        displayStatistics(fetchAllCountries());
                        deleteCountry();
                        break;
                    case "quit":
                        System.out.println("See you later alligator :)");
                        break;
                    default:
                        System.out.printf("Unknown choice: '%s'. Try again.  %n%n%n",
                                choice);
                }


            } catch (IOException ioe) {
                System.out.println("Problem with input");
                ioe.printStackTrace();
            }

        } while (!choice.equals("quit"));
    }



}

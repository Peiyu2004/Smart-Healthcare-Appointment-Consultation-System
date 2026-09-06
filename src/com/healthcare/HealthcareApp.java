package com.healthcare;

import com.healthcare.ui.HealthcareUI;

public class HealthcareApp {
    public static void main(String[] args) {
        HealthcareUI ui = new HealthcareUI();
        ui.start();
    }
}

//package com.healthcare;
//
//import com.healthcare.model.Appointment;
//import com.healthcare.model.ScheduleSlot;
//import com.healthcare.model.User;
//import com.healthcare.service.AppointmentService;
//import com.healthcare.service.UserService;
//
//import java.util.List;
//import java.util.Scanner;
//
//public class HealthcareApp {
//    private static UserService userService = new UserService();
//    private static AppointmentService appointmentService = new AppointmentService();
//    private static User currentUser = null;
//
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("==================================================");
//        System.out.println(" Welcome to Smart Healthcare System ");
//        System.out.println("==================================================");
//
//        while (true) {
//            if (currentUser == null) {
//                showAuthMenu(scanner);
//            } else {
//                showMainMenu(scanner);
//            }
//        }
//    }
//
//    private static void showAuthMenu(Scanner scanner) {
//        System.out.println("\n--- Authentication Menu ---");
//        System.out.println("1. Login");
//        System.out.println("2. Register New Patient");
//        System.out.println("3. Exit");
//        System.out.print("Select an option: ");
//
//        String choice = scanner.nextLine();
//        switch (choice) {
//            case "1":
//                login(scanner);
//                break;
//            case "2":
//                register(scanner);
//                break;
//            case "3":
//                System.out.println("Exiting system. Goodbye!");
//                System.exit(0);
//                break;
//            default:
//                System.out.println("Invalid option. Please try again.");
//        }
//    }
//
//    private static void login(Scanner scanner) {
//        System.out.print("\nEnter Email: ");
//        String email = scanner.nextLine();
//        System.out.print("Enter Password Hash/Password: ");
//        String password = scanner.nextLine();
//
//        User user = userService.authenticate(email, password);
//        if (user != null) {
//            currentUser = user;
//            System.out.println("\nLogin successful! Welcome, " + currentUser.getFullName() + " [" + currentUser.getRoleLabel() + "]");
//        } else {
//            System.out.println("Invalid email or password.");
//        }
//    }
//
//    private static void register(Scanner scanner) {
//        System.out.println("\n--- Patient Registration ---");
//        System.out.print("Full Name: ");
//        String fullName = scanner.nextLine();
//        System.out.print("Email: ");
//        String email = scanner.nextLine();
//        System.out.print("Phone Number: ");
//        String phone = scanner.nextLine();
//        System.out.print("Password Hash: ");
//        String pass = scanner.nextLine();
//        System.out.print("Date of Birth (YYYY-MM-DD): ");
//        String dob = scanner.nextLine();
//        System.out.print("Gender: ");
//        String gender = scanner.nextLine();
//        System.out.print("Address: ");
//        String address = scanner.nextLine();
//
//        User registered = userService.registerPatient(fullName, email, phone, pass, dob, gender, address);
//        if (registered != null) {
//            System.out.println("Registration successful! You can now login.");
//        }
//    }
//
//    private static void showMainMenu(Scanner scanner) {
//        System.out.println("\n--- Main Menu ---");
//        System.out.println("1. View Available Schedule Slots");
//        System.out.println("2. Book an Appointment");
//        System.out.println("3. View My Appointments");
//        System.out.println("4. Cancel an Appointment");
//        System.out.println("5. Logout");
//        System.out.print("Select an option: ");
//
//        String choice = scanner.nextLine();
//        switch (choice) {
//            case "1":
//                viewSlots();
//                break;
//            case "2":
//                bookAppointment(scanner);
//                break;
//            case "3":
//                viewMyAppointments();
//                break;
//            case "4":
//                cancelAppointment(scanner);
//                break;
//            case "5":
//                System.out.println("Logging out...");
//                currentUser = null;
//                break;
//            default:
//                System.out.println("Invalid choice. Try again.");
//        }
//    }
//
//    private static void viewSlots() {
//        List<ScheduleSlot> slots = appointmentService.getAvailableSlots();
//        System.out.println("\n--- Available Slots ---");
//        if (slots.isEmpty()) {
//            System.out.println("No available slots found.");
//            return;
//        }
//        for (ScheduleSlot slot : slots) {
//            System.out.println("Slot ID: " + slot.getSlotId() + " | Doctor ID: " + slot.getDoctorId() + 
//                               " | Date: " + slot.getSlotDate() + " | Time: " + slot.getStartTime() + " - " + slot.getEndTime());
//        }
//    }
//
//    private static void bookAppointment(Scanner scanner) {
//        viewSlots();
//        System.out.print("\nEnter Slot ID to book: ");
//        String slotId = scanner.nextLine();
//        System.out.print("Enter Doctor ID: ");
//        String doctorId = scanner.nextLine();
//        System.out.print("Enter Reason for Visit: ");
//        String reason = scanner.nextLine();
//
//        Appointment app = appointmentService.bookAppointment(currentUser.getUserId(), doctorId, slotId, reason);
//        if (app != null) {
//            System.out.println("Appointment booked successfully! ID: " + app.getAppointmentId());
//        }
//    }
//
//    private static void viewMyAppointments() {
//        List<Appointment> apps = appointmentService.getAppointmentsByPatient(currentUser.getUserId());
//        System.out.println("\n--- My Appointments ---");
//        if (apps.isEmpty()) {
//            System.out.println("No appointments found.");
//            return;
//        }
//        for (Appointment a : apps) {
//            System.out.println("App ID: " + a.getAppointmentId() + " | Doctor ID: " + a.getDoctorId() + 
//                               " | Status: " + a.getStatus() + " | Reason: " + a.getReasonForVisit());
//        }
//    }
//
//    private static void cancelAppointment(Scanner scanner) {
//        viewMyAppointments();
//        System.out.print("\nEnter Appointment ID to cancel: ");
//        String appID = scanner.nextLine();
//        System.out.print("Enter Cancellation Reason: ");
//        String reason = scanner.nextLine();
//
//        if (appointmentService.cancelAppointment(appID, reason)) {
//            System.out.println("Appointment cancelled successfully.");
//        } else {
//            System.out.println("Failed to cancel appointment. Check ID or status.");
//        }
//    }
//}
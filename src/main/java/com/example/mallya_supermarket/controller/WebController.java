package com.example.mallya_supermarket.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/cashier/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "dashboard");
        return "admin-dashboard";
    }

    @GetMapping("/admin/employees/new")
    public String adminAddEmployee(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "add-employee");
        return "admin-add-employee";
    }

    @GetMapping("/admin/products/new")
    public String adminAddProduct(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "add-product");
        return "admin-add-product";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "products");
        return "admin-products";
    }

    @GetMapping("/admin/sales")
    public String adminSales(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "sales");
        return "admin-sales";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "reports");
        return "admin-reports";
    }

    @GetMapping("/admin/employees")
    public String adminEmployees(Authentication authentication, Model model) {
        addAdminModel(authentication, model, "employees");
        return "admin-employees";
    }

    @GetMapping("/admin/record-sale")
    public String adminRecordSale() {
        return "redirect:/cashier/select-products";
    }

    @GetMapping("/cashier/dashboard")
    public String cashierDashboard(Authentication authentication, Model model) {
        addCashierModel(authentication, model, "dashboard");
        return "cashier-dashboard";
    }

    @GetMapping("/cashier/select-products")
    public String cashierSelectProducts(Authentication authentication, Model model) {
        addCashierModel(authentication, model, "select-products");
        return "cashier-select-products";
    }

    @GetMapping("/cashier/checkout")
    public String cashierCheckout(Authentication authentication, Model model) {
        addCashierModel(authentication, model, "checkout");
        return "cashier-checkout";
    }

    @GetMapping("/cashier/receipt")
    public String cashierReceipt(Authentication authentication, Model model) {
        addCashierModel(authentication, model, "receipt");
        return "cashier-receipt";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }

    private void addAdminModel(Authentication authentication, Model model, String activePage) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("role", "ADMIN");
        model.addAttribute("activePage", activePage);
    }

    private void addCashierModel(Authentication authentication, Model model, String activePage) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("role", hasRole(authentication, "ADMIN") ? "ADMIN" : "CASHIER");
        model.addAttribute("activePage", activePage);
    }
}

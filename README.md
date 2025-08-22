@GetMapping("/dashboard")
public String showDashboard(Model model, Authentication authentication) {
String username = authentication.getName();
User user = userService.findByUsername(username)
.orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Optional<Customer> customerOpt = customerService.findByUser(user);
        Customer customer = customerOpt.orElse(null);

        List<Account> accounts = customer != null ? accountService.findByCustomer(customer) : List.of();

        BigDecimal totalBalance = accounts.stream()
                .map(acc -> acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // Prepare data for chart (simple lists instead of Java streams in template)
        List<String> chartLabels = accounts.stream()
                .map(acc -> "Acc-" + acc.getAccountId())
                .collect(Collectors.toList());

        List<BigDecimal> chartBalances = accounts.stream()
                .map(acc -> acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("accounts", accounts);
        model.addAttribute("totalAccounts", accounts.size());
        model.addAttribute("totalBalance", totalBalance);

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartBalances", chartBalances);

        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }
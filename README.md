# Mallya Supermarket Management System

A comprehensive Spring Boot application for managing supermarket inventory, point-of-sale operations, and reporting with smart expiration tracking.

## Features

###  Point of Sale (POS)
- **Barcode Scanning**: Fast product lookup via barcode
- **Stock Validation**: Prevents sales of out-of-stock items
- **Expiration Blocking**: Stops sale of expired products
- **Transaction Processing**: Complete sales with tax calculation
- **Receipt Generation**: Automatic receipt creation

###  Smart Inventory Management
- **Automated Expiration Tracking**: Midnight scheduled tasks update expired status
- **Visual Alerts**: Dashboard notifications for products expiring within 30 days
- **Low Stock Monitoring**: Configurable thresholds for reorder alerts
- **Category Management**: Organized product categorization

###  Reporting & Analytics
- **Revenue Reports**: Daily and monthly income tracking
- **Wastage Reports**: Loss calculation from expired products
- **Low Stock Reports**: Items needing immediate reorder
- **Dashboard Summary**: Real-time key metrics

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Java Version**: 21
- **Database**: MySQL (Port 3307 via XAMPP)
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security
- **Frontend**: Thymeleaf + HTML/CSS/JavaScript
- **Build Tool**: Maven
- **Additional**: Lombok for boilerplate reduction

## Database Schema

### Products Table
```sql
- id (Primary Key)
- barcode (Indexed for fast scanning)
- name, description
- category_id (Foreign Key)
- cost_price, selling_price
- stock_quantity
- expiry_date (Date type)
- is_expired (Boolean/Flag)
```

### Sales Tables
```sql
sales: id, total_price, tax, timestamp, cashier_id
sale_items: id, sale_id, product_id, quantity, price_at_sale
```

## Setup Instructions

### Prerequisites
1. **Java 21** installed
2. **Maven** installed
3. **XAMPP** with MySQL running on **Port 3307**

### Database Setup
1. Start XAMPP and ensure MySQL is running on port 3307
2. Create a database named `mallya_supermarket`
3. Update `application.properties` if using different credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3307/mallya_supermarket
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

### Application Setup
1. Clone or navigate to the project directory
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
3. Access the dashboard at: `http://localhost:8080`

## API Endpoints

### Point of Sale
- `POST /api/pos/scan` - Scan product by barcode
- `POST /api/pos/sale` - Complete a sale transaction

### Inventory Management
- `GET /api/inventory/expiring-soon` - Products expiring within 30 days
- `GET /api/inventory/expired` - All expired products
- `GET /api/inventory/low-stock` - Low stock items
- `GET /api/inventory/alerts` - All inventory alerts

### Reports
- `GET /api/reports/dashboard` - Dashboard summary
- `GET /api/reports/revenue/{period}` - Revenue report (daily/monthly)
- `GET /api/reports/wastage` - Wastage report
- `GET /api/reports/low-stock` - Low stock report

## Sample Data

The application automatically initializes with sample data on first startup:
- **Categories**: Beverages, Dairy, Snacks, Bakery
- **Products**: 10 sample products with various expiry dates
- **Users**: Admin and cashier accounts
- **Expired Products**: 2 expired products for testing

## Usage Examples

### Scanning a Product
```bash
curl -X POST http://localhost:8080/api/pos/scan \
  -H "Content-Type: application/json" \
  -d '{"barcode": "1234567890"}'
```

### Completing a Sale
```bash
curl -X POST http://localhost:8080/api/pos/sale \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"barcode": "1234567890", "quantity": 2},
      {"barcode": "1234567891", "quantity": 1}
    ],
    "cashierUsername": "admin"
  }'
```

## Scheduled Tasks

The system includes an automated expiration check that runs every midnight:
- Updates `is_expired` flag for products past their expiry date
- Logs the number of products marked as expired
- Prevents accidental sale of expired items

## Security Configuration

The application is configured with:
- Password encryption using BCrypt
- API endpoints accessible without authentication (for demo purposes)
- Easy to extend with proper authentication for production

## Development Notes

### Key Features Implemented
1.  **Expiration Management**: Automated tracking and alerts
2.  **POS Workflow**: Complete barcode-based sales process
3.  **Dashboard**: Real-time metrics and reporting
4.  **Database Schema**: Proper entity relationships
5.  **Sample Data**: Automatic initialization for testing

### Architecture Highlights
- **Service Layer**: Business logic separated from controllers
- **Repository Layer**: Data access with custom queries
- **Entity Relationships**: Proper JPA mappings
- **Error Handling**: Comprehensive exception handling
- **Logging**: Detailed logging for debugging

## Future Enhancements

- User authentication and role-based access
- Advanced inventory forecasting
- Supplier management
- Customer loyalty programs
- Mobile POS interface
- Advanced reporting with charts
- Barcode label printing
- Integration with payment gateways

## Troubleshooting

### Common Issues
1. **Database Connection**: Ensure MySQL is running on port 3307
2. **Sample Data Not Loading**: Check database permissions
3. **Expired Products**: Verify system date/time settings

### Logs
Check the application logs for detailed error messages and scheduled task execution.

## License

This project is for demonstration purposes as part of the Mallya Supermarket management system requirements.

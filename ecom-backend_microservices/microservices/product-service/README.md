# Product Service

## Overview
The Product Service is a core microservice in the e-commerce platform responsible for managing product catalog, categories, inventory tracking, and advanced search capabilities. It integrates with Elasticsearch for powerful search features and publishes events for inventory changes.

## Architecture Position
```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   Frontend  │────▶│ API Gateway │────▶│Product Service│
└─────────────┘     └─────────────┘     └──────────────┘
                                               │
                                    ┌──────────┴───────────┐
                                    │          │           │
                               ┌────▼───┐ ┌───▼───┐ ┌─────▼──────┐
                               │PostgreSQL│ │ Redis │ │Elasticsearch│
                               └────────┘ └───────┘ └────────────┘
```

## Technologies Used

### Core Framework
- **Spring Boot 3.2.0** - Microservice framework
- **Spring Data JPA** - ORM for PostgreSQL operations
- **Spring Data Elasticsearch** - Integration with Elasticsearch

### Data Storage & Search
- **PostgreSQL 15** - Primary database for product data
  - Why: ACID compliance, relational data modeling, JSON support
- **Elasticsearch 8.11** - Full-text search engine
  - Why: Advanced search, faceted search, auto-complete, relevance scoring
- **Redis 7** - Caching layer
  - Why: High-performance caching for frequently accessed products

### Service Communication
- **Eureka Client** - Service registration
  - Why: Dynamic service discovery
- **Spring Cloud Config Client** - Configuration management
  - Why: Centralized configuration
- **Apache Kafka** - Event streaming
  - Why: Asynchronous event publishing for inventory changes

### Security & Monitoring
- **Spring Security** - API security
  - Why: Role-based access control for admin operations
- **Spring Boot Actuator** - Monitoring
  - Why: Health checks and metrics
- **Micrometer** - Metrics collection
  - Why: Prometheus and Zipkin integration

### Development Tools
- **Lombok** - Boilerplate reduction
  - Why: Cleaner code with annotations
- **ModelMapper** - Object mapping
  - Why: Easy DTO conversions
- **Swagger/OpenAPI 3** - API documentation
  - Why: Interactive API documentation

## Project Structure

```
product-service/
├── src/main/java/com/ecommerce/product/
│   ├── ProductServiceApplication.java         # Main application class
│   ├── config/
│   │   ├── ApplicationConfig.java             # Bean configurations
│   │   ├── ElasticsearchConfig.java           # Elasticsearch settings
│   │   └── ElasticsearchInitializer.java      # Index initialization
│   ├── controller/
│   │   ├── ProductController.java             # Product REST endpoints
│   │   ├── CategoryController.java            # Category REST endpoints
│   │   └── ProductSearchController.java       # Search endpoints
│   ├── dto/                                   # Data Transfer Objects
│   │   ├── ProductDTO.java                    # Product representation
│   │   ├── CategoryDTO.java                   # Category representation
│   │   ├── SearchCriteria.java                # Search parameters
│   │   └── ProductResponse.java               # Paginated response
│   ├── event/
│   │   ├── ProductEvent.java                  # Base product event
│   │   ├── ProductCreatedEvent.java           # Creation event
│   │   ├── ProductUpdatedEvent.java           # Update event
│   │   └── StockUpdatedEvent.java             # Inventory event
│   ├── exception/
│   │   ├── ResourceNotFoundException.java     # 404 errors
│   │   ├── ProductException.java              # Business errors
│   │   └── GlobalExceptionHandler.java        # Error handling
│   ├── model/
│   │   ├── Product.java                       # Product entity
│   │   ├── Category.java                      # Category entity
│   │   └── elasticsearch/
│   │       └── ProductDocument.java           # Elasticsearch document
│   ├── repository/
│   │   ├── jpa/
│   │   │   ├── ProductRepository.java         # Product JPA repo
│   │   │   └── CategoryRepository.java        # Category JPA repo
│   │   └── elasticsearch/
│   │       └── ProductSearchRepository.java   # Elasticsearch repo
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java       # JWT validation
│   │   ├── JwtAuthenticationToken.java        # Custom auth token
│   │   ├── JwtAuthenticationEntryPoint.java   # Unauthorized handler
│   │   └── SecurityConfig.java                # Security configuration
│   └── service/
│       ├── ProductService.java                # Product interface
│       ├── ProductServiceImpl.java            # Product implementation
│       ├── CategoryService.java               # Category interface
│       ├── CategoryServiceImpl.java           # Category implementation
│       ├── ProductSearchService.java          # Search interface
│       ├── ProductSearchServiceImpl.java      # Search implementation
│       └── FileService.java                   # Image handling
├── src/main/resources/
│   ├── bootstrap.yml                          # Config server connection
│   └── static/images/                         # Product images
└── pom.xml                                    # Maven dependencies
```

## Key Components

### 1. ProductController
Handles product CRUD operations and inventory management:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    public ResponseEntity<ProductResponse> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice)
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id)
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<ProductDTO> createProduct(
        @Valid @RequestBody ProductDTO productDTO)
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<ProductDTO> updateProduct(
        @PathVariable Long id,
        @Valid @RequestBody ProductDTO productDTO)
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id)
    
    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<ProductDTO> updateStock(
        @PathVariable Long id,
        @RequestParam Integer quantity,
        @RequestParam String operation)
    
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<String> uploadProductImage(
        @PathVariable Long id,
        @RequestParam("image") MultipartFile file)
}
```

### 2. ProductSearchController
Advanced search capabilities with Elasticsearch:

```java
@RestController
@RequestMapping("/api/products/search")
public class ProductSearchController {
    
    @GetMapping
    public ResponseEntity<Page<ProductDocument>> searchProducts(
        @RequestParam String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size)
    
    @PostMapping("/advanced")
    public ResponseEntity<Page<ProductDocument>> advancedSearch(
        @RequestBody SearchCriteria criteria,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size)
    
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(
        @RequestParam String prefix,
        @RequestParam(defaultValue = "10") int limit)
    
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<ProductDocument>> findSimilarProducts(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "5") int limit)
}
```

### 3. Product Entity
JPA entity with comprehensive product attributes:

```java
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    
    @NotBlank
    @Size(min = 3, max = 200)
    private String productName;
    
    @Lob
    private String description;
    
    @NotNull
    @DecimalMin("0.0")
    private Double price;
    
    @Column(precision = 5, scale = 2)
    private Double discountPercentage = 0.0;
    
    @NotNull
    @Min(0)
    private Integer quantity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    private String image;
    private Long sellerId;
    private String brand;
    private Boolean active = true;
    
    @Column(precision = 3, scale = 2)
    private Double rating = 0.0;
    
    private Integer reviewCount = 0;
    
    @Column(unique = true)
    private String sku;
    
    @ElementCollection
    private List<String> tags;
    
    private Integer minOrderQuantity = 1;
    private Integer maxOrderQuantity = 100;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

## Database Schema

### Tables

#### products
| Column | Type | Description |
|--------|------|-------------|
| product_id | BIGINT | Primary key |
| product_name | VARCHAR(200) | Product name |
| description | TEXT | Detailed description |
| price | DECIMAL(10,2) | Base price |
| discount_percentage | DECIMAL(5,2) | Discount rate |
| quantity | INTEGER | Stock quantity |
| category_id | BIGINT | Foreign key to categories |
| image | VARCHAR(255) | Image URL |
| seller_id | BIGINT | Seller identifier |
| brand | VARCHAR(100) | Brand name |
| active | BOOLEAN | Product status |
| rating | DECIMAL(3,2) | Average rating |
| review_count | INTEGER | Number of reviews |
| sku | VARCHAR(50) | Stock keeping unit |
| min_order_quantity | INTEGER | Minimum order |
| max_order_quantity | INTEGER | Maximum order |
| created_at | TIMESTAMP | Creation date |
| updated_at | TIMESTAMP | Last update |

#### categories
| Column | Type | Description |
|--------|------|-------------|
| category_id | BIGINT | Primary key |
| category_name | VARCHAR(100) | Category name |
| description | TEXT | Category description |
| image | VARCHAR(255) | Category image |
| parent_id | BIGINT | Parent category |
| active | BOOLEAN | Category status |
| display_order | INTEGER | Display order |
| created_at | TIMESTAMP | Creation date |
| updated_at | TIMESTAMP | Last update |

#### product_tags
| Column | Type | Description |
|--------|------|-------------|
| product_product_id | BIGINT | Foreign key to products |
| tags | VARCHAR(255) | Tag value |

## Elasticsearch Schema

### ProductDocument
```json
{
  "productId": 123,
  "productName": "Wireless Headphones",
  "description": "High-quality noise-cancelling headphones",
  "brand": "TechBrand",
  "price": 299.99,
  "discountPercentage": 10.0,
  "discountedPrice": 269.99,
  "quantity": 50,
  "categoryId": 5,
  "categoryName": "Electronics",
  "categoryHierarchy": ["Electronics", "Audio", "Headphones"],
  "sku": "WH-12345",
  "imageUrl": "/images/products/wh-12345.jpg",
  "sellerId": 10,
  "rating": 4.5,
  "reviewCount": 125,
  "active": true,
  "featured": true,
  "inStock": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-20T15:45:00Z"
}
```

## API Endpoints

### Product Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/products` | List products with pagination | No |
| GET | `/api/products/{id}` | Get product details | No |
| POST | `/api/products` | Create new product | ADMIN/SELLER |
| PUT | `/api/products/{id}` | Update product | ADMIN/SELLER |
| DELETE | `/api/products/{id}` | Delete product | ADMIN |
| PUT | `/api/products/{id}/stock` | Update stock | ADMIN/SELLER |
| POST | `/api/products/{id}/image` | Upload image | ADMIN/SELLER |
| GET | `/api/products/seller/{sellerId}` | Get seller products | No |
| GET | `/api/products/featured` | Get featured products | No |

### Category Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/categories` | List all categories | No |
| GET | `/api/categories/{id}` | Get category details | No |
| POST | `/api/categories` | Create category | ADMIN |
| PUT | `/api/categories/{id}` | Update category | ADMIN |
| DELETE | `/api/categories/{id}` | Delete category | ADMIN |
| GET | `/api/categories/tree` | Get category hierarchy | No |

### Search Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/products/search` | Basic search | No |
| POST | `/api/products/search/advanced` | Advanced search | No |
| GET | `/api/products/search/autocomplete` | Auto-complete | No |
| GET | `/api/products/search/similar/{id}` | Similar products | No |

## Configuration

### Environment Variables
```yaml
# Database
POSTGRES_HOST: localhost
POSTGRES_PORT: 5432
POSTGRES_DB: product_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Elasticsearch
ELASTICSEARCH_URIS: http://localhost:9200
ELASTICSEARCH_USERNAME: elastic
ELASTICSEARCH_PASSWORD: elastic

# Kafka
KAFKA_BOOTSTRAP_SERVERS: localhost:9092

# File Upload
UPLOAD_DIR: ./uploads/products
MAX_FILE_SIZE: 5MB
```

### Application Properties
```yaml
server:
  port: 8083

spring:
  application:
    name: product-service
  
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/product_db
  
  jpa:
    hibernate:
      ddl-auto: update
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
    username: ${ELASTICSEARCH_USERNAME:}
    password: ${ELASTICSEARCH_PASSWORD:}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      topic:
        product-events: product-events
  
  servlet:
    multipart:
      max-file-size: ${MAX_FILE_SIZE:5MB}
      max-request-size: ${MAX_FILE_SIZE:5MB}

app:
  upload:
    dir: ${UPLOAD_DIR:./uploads/products}
```

## Events Published

### PRODUCT_CREATED
```json
{
  "eventType": "PRODUCT_CREATED",
  "timestamp": "2024-01-20T10:30:00Z",
  "productId": 123,
  "productName": "Wireless Headphones",
  "categoryId": 5,
  "price": 299.99,
  "quantity": 50,
  "sellerId": 10
}
```

### PRODUCT_UPDATED
```json
{
  "eventType": "PRODUCT_UPDATED",
  "timestamp": "2024-01-20T11:00:00Z",
  "productId": 123,
  "changedFields": ["price", "discountPercentage"],
  "oldValues": {
    "price": 299.99,
    "discountPercentage": 0.0
  },
  "newValues": {
    "price": 299.99,
    "discountPercentage": 10.0
  }
}
```

### STOCK_UPDATED
```json
{
  "eventType": "STOCK_UPDATED",
  "timestamp": "2024-01-20T11:30:00Z",
  "productId": 123,
  "oldQuantity": 50,
  "newQuantity": 45,
  "operation": "DECREASE",
  "changeAmount": 5,
  "reason": "ORDER_PLACED"
}
```

## Caching Strategy

### Redis Cache Keys
- `product::{productId}` - Individual product (TTL: 1 hour)
- `products::page:{page}:size:{size}` - Product list (TTL: 15 minutes)
- `categories::all` - All categories (TTL: 1 hour)
- `category::{categoryId}` - Individual category (TTL: 1 hour)
- `product::featured` - Featured products (TTL: 30 minutes)

### Cache Eviction
- On product update: Evict specific product and related lists
- On category update: Evict category and all product lists
- On stock update: Evict product cache

## Search Features

### 1. Full-Text Search
- Search across product name, description, brand
- Relevance scoring based on term frequency
- Fuzzy matching for typo tolerance

### 2. Faceted Search
- Filter by category, brand, price range
- Multi-select filters
- Dynamic facet counts

### 3. Auto-Complete
- Prefix matching on product names
- Suggestion ranking by popularity
- Real-time suggestions

### 4. Similar Products
- Based on category and attributes
- Elasticsearch More Like This query
- Configurable similarity threshold

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

### Manual Testing Examples

1. **Create Product:**
```bash
curl -X POST http://localhost:8083/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "price": 49.99,
    "quantity": 100,
    "categoryId": 3,
    "brand": "TechBrand",
    "sku": "WM-001"
  }'
```

2. **Search Products:**
```bash
curl -X GET "http://localhost:8083/api/products/search?query=wireless&page=0&size=10"
```

3. **Update Stock:**
```bash
curl -X PUT "http://localhost:8083/api/products/123/stock?quantity=5&operation=DECREASE" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Performance Optimizations

### 1. Database
- Indexes on frequently queried columns
- Lazy loading for relationships
- Query optimization with JPQL

### 2. Caching
- Redis for frequently accessed products
- Cache warming for popular items
- Selective cache invalidation

### 3. Elasticsearch
- Asynchronous indexing
- Bulk operations for reindexing
- Index optimization settings

### 4. API Response
- Pagination for large datasets
- Projection for minimal data transfer
- Compression for responses

## Monitoring

### Health Endpoints
- `/actuator/health` - Overall health
- `/actuator/health/db` - Database health
- `/actuator/health/redis` - Redis health
- `/actuator/health/elasticsearch` - Elasticsearch health

### Metrics
- Product creation rate
- Search query performance
- Cache hit/miss ratio
- Stock update frequency

## Troubleshooting

### Common Issues

1. **Elasticsearch Connection Failed**
   - Verify Elasticsearch is running
   - Check connection credentials
   - Ensure index exists

2. **Image Upload Failed**
   - Check file size limits
   - Verify upload directory permissions
   - Ensure sufficient disk space

3. **Stock Inconsistency**
   - Check Kafka event publishing
   - Verify transaction boundaries
   - Monitor concurrent updates

## Best Practices

1. **Data Integrity**
   - Use database transactions for stock updates
   - Implement optimistic locking
   - Validate business rules

2. **Search Optimization**
   - Regular index optimization
   - Monitor search performance
   - Use appropriate analyzers

3. **Security**
   - Validate file uploads
   - Sanitize search inputs
   - Implement rate limiting

## Future Enhancements

1. **Advanced Features**
   - Product variants (size, color)
   - Bundle products
   - Dynamic pricing rules
   - Inventory forecasting

2. **Search Improvements**
   - Machine learning for relevance
   - Personalized search results
   - Visual search capabilities
   - Voice search integration

3. **Performance**
   - GraphQL API support
   - Read replicas for scaling
   - CDN integration for images
   - Distributed caching 
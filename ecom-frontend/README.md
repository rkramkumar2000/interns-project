# E-Commerce Frontend Application

A modern, feature-rich e-commerce frontend application built with React, Redux, and Tailwind CSS. This application provides a complete shopping experience with user authentication, product browsing, cart management, and secure payment processing.

## 🚀 Features

### Customer Features
- **User Authentication**: Secure login and registration system
- **Product Browsing**: Browse products with advanced filtering and pagination
- **Shopping Cart**: Full cart management with persistent storage
- **Checkout Process**: Multi-step checkout with address management
- **Payment Integration**: Stripe payment processing (PayPal placeholder)
- **Responsive Design**: Mobile-first approach with responsive layouts

### Admin Features
- **Dashboard**: Analytics overview with key metrics
- **Product Management**: Add, edit, and manage products with image upload
- **Category Management**: Create and manage product categories
- **Order Management**: View and update order statuses
- **Seller Management**: Manage seller accounts (for multi-vendor support)
- **Role-Based Access**: Admin and seller role differentiation

## 🛠️ Tech Stack

### Core Technologies
- **React 18.3.1**: UI library for building user interfaces
- **Redux Toolkit 2.3.0**: State management solution
- **React Router DOM 7.0.1**: Client-side routing
- **Vite 5.4.10**: Fast build tool and development server
- **Tailwind CSS 4.1.11**: Utility-first CSS framework

### UI Components & Libraries
- **Material UI 6.1.8**: React component library for consistent UI
- **MUI X Data Grid 8.9.1**: Advanced data grid component
- **Headless UI 2.2.0**: Unstyled, accessible UI components
- **Swiper 11.1.15**: Modern touch slider component
- **React Icons 5.3.0**: Popular icon libraries collection

### Form & Validation
- **React Hook Form 7.54.0**: Performant forms with easy validation

### Payment Integration
- **Stripe**: 
  - `@stripe/react-stripe-js 3.1.1`
  - `@stripe/stripe-js 5.6.0`

### HTTP Client
- **Axios 1.7.7**: Promise-based HTTP client

### Other Utilities
- **React Hot Toast 2.4.1**: Toast notifications
- **React Loader Spinner 6.1.6**: Loading animations
- **Classnames 2.5.1**: Conditional CSS classes utility

## 📁 Project Structure

```
ecom-frontend/
├── src/
│   ├── api/                    # API configuration
│   │   └── api.js             # Axios instance with base configuration
│   ├── assets/                # Static assets
│   │   └── sliders/           # Banner images
│   ├── components/            # React components
│   │   ├── admin/             # Admin panel components
│   │   │   ├── categories/    # Category management
│   │   │   ├── dashboard/     # Dashboard and analytics
│   │   │   ├── orders/        # Order management
│   │   │   ├── products/      # Product management
│   │   │   └── sellers/       # Seller management
│   │   ├── auth/              # Authentication components
│   │   │   ├── LogIn.jsx
│   │   │   └── Register.jsx
│   │   ├── cart/              # Shopping cart components
│   │   ├── checkout/          # Checkout process components
│   │   │   ├── AddressInfo.jsx
│   │   │   ├── PaymentMethod.jsx
│   │   │   ├── StripePayment.jsx
│   │   │   └── OrderSummary.jsx
│   │   ├── home/              # Homepage components
│   │   ├── products/          # Product listing and filtering
│   │   ├── shared/            # Reusable components
│   │   └── PrivateRoute.jsx   # Route protection component
│   ├── hooks/                 # Custom React hooks
│   │   ├── useCategoryFilter.js
│   │   ├── useOrderFilter.js
│   │   └── useProductFilter.js
│   ├── store/                 # Redux store configuration
│   │   ├── actions/           # Redux actions
│   │   └── reducers/          # Redux reducers
│   │       ├── adminReducer.js
│   │       ├── authReducer.js
│   │       ├── cartReducer.js
│   │       ├── errorReducer.js
│   │       ├── orderReducer.js
│   │       ├── paymentMethodReducer.js
│   │       ├── ProductReducer.js
│   │       ├── sellerReducer.js
│   │       └── store.js       # Store configuration
│   ├── utils/                 # Utility functions
│   ├── App.jsx               # Main app component with routing
│   ├── App.css               # App-specific styles
│   ├── index.css             # Global styles and Tailwind imports
│   └── main.jsx              # Application entry point
├── public/                    # Public assets
├── .gitignore
├── eslint.config.js          # ESLint configuration
├── index.html                # HTML template
├── package.json              # Project dependencies
├── postcss.config.js         # PostCSS configuration
├── README.md                 # This file
└── vite.config.js           # Vite configuration
```

## 🚦 Getting Started

### Prerequisites
- Node.js (v16 or higher)
- npm or yarn package manager
- Backend API server running (Spring Boot backend)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd ecom-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create a `.env` file in the root directory:
```env
VITE_BACK_END_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
```

4. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## 🔐 Authentication & Authorization

### User Roles
1. **Customer**: Can browse products, manage cart, and place orders
2. **Seller**: Access to product and order management
3. **Admin**: Full access to all features including analytics and user management

### Protected Routes
- `/checkout` - Requires authentication
- `/admin/*` - Requires admin or seller role
- Seller-specific paths: `/admin/orders`, `/admin/products`

## 🎨 Styling

The application uses Tailwind CSS v4 with custom configuration:

### Custom Theme Extensions
- **Fonts**: Montserrat font family
- **Colors**: Custom color palette for branding
- **Gradients**: Custom gradient backgrounds
- **Shadows**: Custom shadow utilities

### Key CSS Classes
- `bg-custom-gradient` - Primary gradient background
- `bg-button-gradient` - Button gradient effect
- `shadow-custom` - Custom shadow effect

## 📱 Responsive Design

The application is fully responsive with breakpoints:
- Mobile: Default (<640px)
- Tablet: sm (≥640px)
- Laptop: lg (≥1024px)
- Desktop: xl (≥1280px)
- Large Desktop: 2xl (≥1536px)

## 🔄 State Management

### Redux Store Structure
```javascript
{
  products: {
    products: [],
    categories: [],
    pagination: {}
  },
  auth: {
    user: {},
    address: [],
    selectedUserCheckoutAddress: {}
  },
  carts: {
    cart: [],
    cartId: null,
    totalPrice: 0
  },
  errors: {
    isLoading: false,
    errorMessage: ""
  },
  payment: {
    paymentMethod: ""
  },
  admin: {
    analytics: {}
  },
  order: {
    orders: []
  },
  seller: {
    sellers: []
  }
}
```

### Local Storage
- User authentication data
- Shopping cart items
- Selected checkout address

## 🔌 API Integration

### Base Configuration
- Base URL: Configured via `VITE_BACK_END_URL`
- Credentials: Included for cookie-based authentication
- Axios interceptors for error handling

### Key API Endpoints
- `/api/public/*` - Public endpoints (products, categories)
- `/api/auth/*` - Authentication endpoints
- `/api/user/*` - User-specific endpoints
- `/api/admin/*` - Admin-only endpoints

## 💳 Payment Integration

### Stripe Integration
- Client-side payment form using Stripe Elements
- Payment intent creation on backend
- Secure payment confirmation flow
- Test mode support

### PayPal (Placeholder)
- Currently shows unavailable message
- Infrastructure ready for future implementation

## 🚀 Production Build

1. Build the application:
```bash
npm run build
```

2. The build output will be in the `dist` directory

3. Deploy to your preferred hosting service (Vercel, Netlify, etc.)

## 🔧 Configuration

### Environment Variables
- `VITE_BACK_END_URL` - Backend API URL
- `VITE_STRIPE_PUBLISHABLE_KEY` - Stripe public key

### Vite Configuration
- React plugin for Fast Refresh
- Default port: 5173
- HMR (Hot Module Replacement) enabled

## 📦 Dependencies

### Production Dependencies
See `package.json` for the complete list of dependencies.

### Development Dependencies
- ESLint with React plugins
- Tailwind CSS PostCSS plugin
- Vite React plugin

## 🐛 Known Issues & Limitations

1. PayPal integration is not yet implemented
2. Product image upload requires backend configuration
3. Real-time order tracking not implemented

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request



## 🙏 Acknowledgments

- React documentation
- Tailwind CSS team
- Material-UI contributors

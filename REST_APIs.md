# Billing Engine

**Version:** 1.4.3  
**Description:** Swagger REST APIs for the billing-engine software  


## REST API Endpoints

### Price Preview Controller
| Verb | Path | Task |
|------|------|------|
| POST | `/billing/previewPrice` | calculateOrderPrice |

### Billing Controller
| Verb | Path | Task |
|------|------|------|
| POST | `/billing/bill` | calculateBill |

### Billing Engine Controller
| Verb | Path | Task |
|------|------|------|
| GET | `/engine/info` | getInfo |
| GET | `/engine/health` | getHealth |


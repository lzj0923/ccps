# CCPS Property Operations

CCPS tracks an owner's relationship with a property from purchase before handover through day-to-day operation and eventual disposal.

## Property lifecycle

**Property Holding**:
An owner's time-bounded relationship with one property unit.
_Avoid_: Property type, owner property

**Pre-handover Holding**:
A property holding whose unit has not yet been handed over and whose active concern is the purchase payment plan.
_Avoid_: Unfinished house, payment property

**Operating Holding**:
A handed-over property holding that may receive operating services and hold reserve, income, expense, lease, and maintenance records.
_Avoid_: Normal property, owned house

**Disposed Holding**:
A historical property holding that ended when ownership was transferred or otherwise terminated.
_Avoid_: Deleted property

## Property services

**Rental Service**:
An operating holding's active instruction to offer or manage the property for rent; it is distinct from an actual lease.
_Avoid_: Rental status

**Resale Service**:
An operating holding's active instruction to offer the property for sale; it may coexist with rental and management services.
_Avoid_: Sale status

**Management Service**:
An operating holding's active instruction for day-to-day property management; it may coexist with rental and resale services.
_Avoid_: Managed property type

## Rental operations

**Rental Mandate**:
An owner's time-bounded instruction for CCPS to offer or manage one operating holding for rent. A rental mandate may exist before a lease and is distinct from both the Rental Service flag and the actual Lease.
_Avoid_: Rental status, rental property, management contract (unless the document specifically means the legal contract)

**Lease**:
A time-bounded agreement between a Tenant and a Unit that defines rent, deposit, payment day and occupancy period.
_Avoid_: Rental mandate, rental invoice

**Property Handover**:
The recorded operational acceptance of a Unit, including the date, condition, inventory, keys and responsible parties.
_Avoid_: Property activation, normal property

## Finance operations

**Cashflow Entry**:
An auditable income or expense event linked to a Unit, owner, tenant, vendor or reserve account. A cashflow entry is not deleted after posting; corrections are represented by an adjustment or reversal.
_Avoid_: Payment (a payment is one possible settlement of an entry), expense record when the direction is not known

**Finance Review**:
The controlled decision that moves a finance record from submitted or pending to approved, rejected, paid or voided, with an actor, timestamp and reason.
_Avoid_: Confirmation (unless the decision is specifically receipt confirmation)

## Operational closure

**Offboarding Case**:
A tracked request to pause or end the operational services of a holding, including dependency checks, financial settlement, approvals, notifications and effective date.
_Avoid_: Delete property, archive property, disposed holding

**Disposed Holding**:
A historical property holding that ended when ownership was transferred or otherwise terminated. It is a terminal holding state and must not be used for temporary rental suspension.
_Avoid_: Downlisted property, inactive listing

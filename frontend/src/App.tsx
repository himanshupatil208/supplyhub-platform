
import { useEffect, useMemo, useState } from 'react'
import {
  Activity, ArrowUpRight, Boxes, ChevronRight,
  CircleDollarSign, ClipboardList, LayoutDashboard,
  Package, Plus, RefreshCw, Search, ShoppingCart,
  Truck, Users, X
} from 'lucide-react'
import { api, Product, Stock, Customer, Order, Notice } from './api'

type Tab =
  | 'Overview'
  | 'Orders'
  | 'Catalog'
  | 'Inventory'
  | 'Customers'
  | 'Activity'

const nav: { name: Tab; icon: any }[] = [
  { name: 'Overview', icon: LayoutDashboard },
  { name: 'Orders', icon: ShoppingCart },
  { name: 'Catalog', icon: Package },
  { name: 'Inventory', icon: Boxes },
  { name: 'Customers', icon: Users },
  { name: 'Activity', icon: Activity }
]

function money(n: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(n || 0)
}

function App() {
  const [tab, setTab] = useState<Tab>('Overview')
  const [products, setProducts] = useState<Product[]>([])
  const [stock, setStock] = useState<Stock[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [notices, setNotices] = useState<Notice[]>([])

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState('')
  const [toast, setToast] = useState('')
  const [editingStock, setEditingStock] = useState<Stock | null>(null)

  async function load() {
    setLoading(true)
    setError('')

    try {
      const [p, s, c, o, n] = await Promise.all([
        api<Product[]>('/api/catalog/products'),
        api<Stock[]>('/api/inventory/stock'),
        api<Customer[]>('/api/customers'),
        api<Order[]>('/api/orders'),
        api<Notice[]>('/api/notifications')
      ])

      setProducts(p)
      setStock(s)
      setCustomers(c)
      setOrders(o)
      setNotices(n)
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Unable to connect to API. Start the services with docker compose up --build.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function notify(msg: string) {
    setToast(msg)
    window.setTimeout(() => setToast(''), 3200)
  }

  const revenue = useMemo(
    () =>
      orders
        .filter(o => o.status !== 'CANCELLED')
        .reduce((a, o) => a + o.total, 0),
    [orders]
  )

  const lowStock = stock.filter(s => s.lowStock)

  const stockBySku = useMemo(
    () => new Map(stock.map(s => [s.sku, s])),
    [stock]
  )

  const activeUnstockedProducts = products.filter(
    p => p.active && !stockBySku.has(p.sku)
  )

  function openStockEdit(item: Stock) {
    setEditingStock(item)
    setModal('edit-stock')
  }

  function closeModal() {
    setModal('')
    setEditingStock(null)
  }

  const filteredProducts = products.filter(p =>
    (p.name + ' ' + p.sku + ' ' + p.category)
      .toLowerCase()
      .includes(query.toLowerCase())
  )

  const filteredCustomers = customers.filter(c =>
    (c.name + ' ' + c.company + ' ' + c.email)
      .toLowerCase()
      .includes(query.toLowerCase())
  )

  const filteredOrders = orders.filter(o =>
    (o.customerName + ' ' + o.id + ' ' + o.status)
      .toLowerCase()
      .includes(query.toLowerCase())
  )

  async function submitForm(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()

    const data = Object.fromEntries(
      new FormData(e.currentTarget).entries()
    )

    try {
      if (modal === 'product') {
        await api('/api/catalog/products', {
          method: 'POST',
          body: JSON.stringify({
            ...data,
            unitPrice: Number(data.unitPrice),
            active: true
          })
        })
      }

      if (modal === 'customer') {
        await api('/api/customers', {
          method: 'POST',
          body: JSON.stringify(data)
        })
      }

      if (modal === 'stock') {
        await api('/api/inventory/stock', {
          method: 'POST',
          body: JSON.stringify({
            sku: data.sku,
            warehouse: data.warehouse,
            availableQuantity: Number(data.availableQuantity),
            reorderLevel: Number(data.reorderLevel)
          })
        })
      }

      if (modal === 'edit-stock' && editingStock) {
        await api(
          `/api/inventory/stock/${encodeURIComponent(editingStock.sku)}`,
          {
            method: 'PUT',
            body: JSON.stringify({
              warehouse: data.warehouse,
              availableQuantity: Number(data.availableQuantity),
              reorderLevel: Number(data.reorderLevel)
            })
          }
        )
      }

      if (modal === 'order') {
        const customer = customers.find(
          c => c.id === Number(data.customerId)
        )

        const product = products.find(
          p => p.sku === data.sku
        )

        if (!customer || !product) {
          throw new Error('Choose a valid customer and product')
        }

        await api('/api/orders', {
          method: 'POST',
          body: JSON.stringify({
            customerId: customer.id,
            customerName: customer.name,
            items: [{
              sku: product.sku,
              productName: product.name,
              quantity: Number(data.quantity),
              unitPrice: product.unitPrice
            }]
          })
        })
      }

      closeModal()
      await load()
      notify('Changes saved successfully')
    } catch (e) {
      notify(
        e instanceof Error
          ? e.message
          : 'Could not save record'
      )
    }
  }

  async function updateStatus(id: number, status: string) {
    try {
      await api(`/api/orders/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status })
      })

      await load()
      notify(`Order #${id} updated`)
    } catch (e) {
      notify(
        e instanceof Error
          ? e.message
          : 'Update failed'
      )
    }
  }

  const title =
    tab === 'Overview' ? 'Good morning, Himanshu' : tab

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Boxes size={22} />
          </div>
          <div>
            <b>supplyhub</b>
            <small>OPERATIONS CLOUD</small>
          </div>
        </div>

        <div className="workspace">
          <div className="workspace-avatar">N</div>
          <div>
            <b>Northstar Foods</b>
            <small>Enterprise workspace</small>
          </div>
          <ChevronRight size={15} />
        </div>

        <div className="nav-label">WORKSPACE</div>

        <nav>
          {nav.map(n => (
            <button
              key={n.name}
              className={
                tab === n.name
                  ? 'nav-item active'
                  : 'nav-item'
              }
              onClick={() => {
                setTab(n.name)
                setQuery('')
              }}
            >
              <n.icon size={18} />
              <span>{n.name}</span>
              {n.name === 'Orders' && orders.length > 0 && (
                <em>{orders.length}</em>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="help-card">
            <div className="help-icon">
              <Truck size={18} />
            </div>
            <b>Keep things moving</b>
            <p>
              Your operations at a glance. Manage orders,
              stock, and customers in one place.
            </p>
          </div>

          <div className="profile">
            <div className="profile-avatar">HP</div>
            <div>
              <b>Himanshu Patil</b>
              <small>Workspace admin</small>
            </div>
            <span>•••</span>
          </div>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="breadcrumbs">
            Workspace <span>/</span> <b>{tab}</b>
          </div>

          <div className="top-actions">
            <div className="system-status">
              <i /> All systems operational
            </div>
            <button
              className="icon-button"
              onClick={load}
              title="Refresh"
            >
              <RefreshCw size={17} />
            </button>
            <div className="top-avatar">HP</div>
          </div>
        </header>

        <div className="content">
          <div className="page-heading">
            <div>
              <div className="eyebrow">
                THURSDAY, NOVEMBER 5, 2026
              </div>
              <h1>{title}</h1>
              <p>
                {tab === 'Overview'
                  ? 'Here’s what’s happening across your distribution business today.'
                  : `Manage your ${tab.toLowerCase()} and keep your operations running smoothly.`}
              </p>
            </div>

            <div className="heading-actions">
              <button className="btn-secondary" onClick={load}>
                <RefreshCw size={15} />
                Refresh
              </button>

              <button
                className="btn-primary"
                onClick={() => {
                  setEditingStock(null)
                  setModal(
                    tab === 'Catalog' ? 'product'
                      : tab === 'Inventory' ? 'stock'
                      : tab === 'Customers' ? 'customer'
                      : 'order'
                  )
                }}
              >
                <Plus size={16} />
                {tab === 'Catalog'
                  ? 'Add product'
                  : tab === 'Inventory'
                    ? 'Add stock'
                    : tab === 'Customers'
                      ? 'Add customer'
                      : 'Create order'}
              </button>
            </div>
          </div>

          {error && (
            <div className="error-banner">
              {error}
              <button onClick={load}>Retry</button>
            </div>
          )}

          {loading && <div className="loading-line" />}

          {tab === 'Overview' && (
            <>
              <div className="metrics-grid">
                <Metric
                  label="Gross order value"
                  value={money(revenue)}
                  change={`${orders.length} total orders`}
                  icon={CircleDollarSign}
                  tone="violet"
                />
                <Metric
                  label="Active customers"
                  value={String(customers.filter(
                    c => c.status !== 'INACTIVE'
                  ).length)}
                  change={`${customers.length} registered accounts`}
                  icon={Users}
                  tone="blue"
                />
                <Metric
                  label="Products in catalog"
                  value={String(products.length)}
                  change={`${products.filter(p => p.active).length} active listings`}
                  icon={Package}
                  tone="orange"
                />
                <Metric
                  label="Low stock alerts"
                  value={String(lowStock.length)}
                  change="Needs replenishment"
                  icon={Boxes}
                  tone="red"
                />
              </div>

              <div className="overview-grid">
                <section className="panel orders-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Recent orders</h2>
                      <p>Latest activity from your customers</p>
                    </div>
                    <button
                      className="text-link"
                      onClick={() => setTab('Orders')}
                    >
                      View all <ChevronRight size={15} />
                    </button>
                  </div>
                  <OrdersTable
                    orders={orders.slice(0, 5)}
                    updateStatus={updateStatus}
                  />
                </section>

                <section className="panel stock-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Inventory health</h2>
                      <p>Items that need your attention</p>
                    </div>
                    <button
                      className="text-link"
                      onClick={() => setTab('Inventory')}
                    >
                      View stock <ChevronRight size={15} />
                    </button>
                  </div>

                  {lowStock.length === 0 ? (
                    <div className="empty-small">
                      <div className="success-dot">✓</div>
                      <b>Inventory looks healthy</b>
                      <span>
                        No products are below their reorder level.
                      </span>
                    </div>
                  ) : (
                    lowStock.slice(0, 5).map(s => (
                      <div className="stock-alert" key={s.id}>
                        <div className="product-symbol">
                          <Package size={16} />
                        </div>
                        <div className="stock-info">
                          <b>{s.productName}</b>
                          <small>
                            {s.sku} · {s.warehouse}
                          </small>
                        </div>
                        <div className="stock-count">
                          <b>{s.availableQuantity}</b>
                          <small>left</small>
                        </div>
                      </div>
                    ))
                  )}
                </section>

                <section className="panel activity-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Latest activity</h2>
                      <p>Recent updates across SupplyHub</p>
                    </div>
                    <button
                      className="text-link"
                      onClick={() => setTab('Activity')}
                    >
                      Activity log <ChevronRight size={15} />
                    </button>
                  </div>
                  <ActivityList notices={notices.slice(0, 4)} />
                </section>
              </div>
            </>
          )}

          {tab === 'Orders' && (
            <section className="panel full-panel">
              <Toolbar
                query={query}
                setQuery={setQuery}
                placeholder="Search orders or customers"
              />
              <OrdersTable
                orders={filteredOrders}
                updateStatus={updateStatus}
                full
              />
            </section>
          )}

          {tab === 'Catalog' && (
            <section className="panel full-panel">
              <Toolbar
                query={query}
                setQuery={setQuery}
                placeholder="Search by product, SKU, or category"
              />
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>PRODUCT</th>
                      <th>SKU</th>
                      <th>CATEGORY</th>
                      <th>UNIT PRICE</th>
                      <th>AVAILABLE</th>
                      <th>STOCK HEALTH</th>
                      <th>STATUS</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredProducts.map(p => {
                      const item = stockBySku.get(p.sku)

                      return (
                        <tr key={p.id}>
                          <td>
                            <div className="cell-main">
                              <div className="product-symbol">
                                <Package size={16} />
                              </div>
                              <div>
                                <b>{p.name}</b>
                                <small>{p.description}</small>
                              </div>
                            </div>
                          </td>
                          <td className="mono">{p.sku}</td>
                          <td>{p.category}</td>
                          <td className="strong">
                            {money(p.unitPrice)}
                          </td>
                          <td className="strong">
                            {item ? item.availableQuantity : '—'}
                          </td>
                          <td>
                            {item ? (
                              <Status
                                value={
                                  item.availableQuantity === 0
                                    ? 'OUT OF STOCK'
                                    : item.lowStock
                                      ? 'LOW STOCK'
                                      : 'IN STOCK'
                                }
                              />
                            ) : (
                              <Status value="NOT TRACKED" />
                            )}
                          </td>
                          <td>
                            <Status
                              value={p.active ? 'ACTIVE' : 'INACTIVE'}
                            />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>

                {filteredProducts.length === 0 && (
                  <Empty label="No products found" />
                )}
              </div>
            </section>
          )}

          {tab === 'Inventory' && (
            <section className="panel full-panel">
              <Toolbar
                query={query}
                setQuery={setQuery}
                placeholder="Search inventory"
              />

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>PRODUCT</th>
                      <th>SKU</th>
                      <th>WAREHOUSE</th>
                      <th>AVAILABLE</th>
                      <th>RESERVED</th>
                      <th>REORDER AT</th>
                      <th>HEALTH</th>
                      <th>ACTIONS</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stock
                      .filter(s =>
                        (s.productName + s.sku + s.warehouse)
                          .toLowerCase()
                          .includes(query.toLowerCase())
                      )
                      .map(s => (
                        <tr key={s.id}>
                          <td>
                            <b>
                              {products.find(
                                p => p.sku === s.sku
                              )?.name || s.productName}
                            </b>
                            {!products.some(
                              p => p.sku === s.sku
                            ) && (
                              <small>
                                Missing from Catalog — create
                                product before editing
                              </small>
                            )}
                          </td>
                          <td className="mono">{s.sku}</td>
                          <td>{s.warehouse}</td>
                          <td className="strong">
                            {s.availableQuantity}
                          </td>
                          <td>{s.reservedQuantity}</td>
                          <td>{s.reorderLevel}</td>
                          <td>
                            <Status
                              value={
                                s.availableQuantity === 0
                                  ? 'OUT OF STOCK'
                                  : s.lowStock
                                    ? 'LOW STOCK'
                                    : 'HEALTHY'
                              }
                            />
                          </td>
                          <td>
                            <button
                              type="button"
                              className="btn-secondary"
                              onClick={() => openStockEdit(s)}
                            >
                              Edit stock
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>

                {stock.length === 0 && (
                  <Empty label="No inventory records yet" />
                )}
              </div>
            </section>
          )}

          {tab === 'Customers' && (
            <section className="panel full-panel">
              <Toolbar
                query={query}
                setQuery={setQuery}
                placeholder="Search customers"
              />

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>CUSTOMER</th>
                      <th>COMPANY</th>
                      <th>EMAIL</th>
                      <th>PHONE</th>
                      <th>STATUS</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredCustomers.map(c => (
                      <tr key={c.id}>
                        <td>
                          <div className="cell-main">
                            <div className="customer-avatar">
                              {c.name.slice(0, 1).toUpperCase()}
                            </div>
                            <b>{c.name}</b>
                          </div>
                        </td>
                        <td>{c.company}</td>
                        <td>{c.email}</td>
                        <td>{c.phone}</td>
                        <td>
                          <Status value={c.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {filteredCustomers.length === 0 && (
                  <Empty label="No customers found" />
                )}
              </div>
            </section>
          )}

          {tab === 'Activity' && (
            <section className="panel full-panel">
              <div className="panel-heading">
                <div>
                  <h2>Activity timeline</h2>
                  <p>Events recorded by your services</p>
                </div>
              </div>

              <ActivityList notices={notices} />

              {notices.length === 0 && (
                <Empty label="No activity recorded yet" />
              )}
            </section>
          )}

          <footer>
            © 2026 SupplyHub Technologies
            <span>Built for modern distribution operations</span>
          </footer>
        </div>
      </main>

      {modal && (
        <div
          className="modal-backdrop"
          onMouseDown={e => {
            if (e.target === e.currentTarget) {
              closeModal()
            }
          }}
        >
          <form className="modal" onSubmit={submitForm}>
            <div className="modal-head">
              <div>
                <div className="eyebrow">
                  SUPPLYHUB WORKSPACE
                </div>
                <h2>
                  {modal === 'product'
                    ? 'Add product'
                    : modal === 'stock'
                      ? 'Add inventory stock'
                      : modal === 'edit-stock'
                        ? 'Edit inventory stock'
                        : modal === 'customer'
                          ? 'Add customer'
                          : 'Create order'}
                </h2>
              </div>

              <button
                type="button"
                className="icon-button"
                onClick={closeModal}
              >
                <X size={18} />
              </button>
            </div>

            {modal === 'product' && (
              <>
                <Field
                  name="sku"
                  label="SKU"
                  placeholder="e.g. LAP-003"
                />
                <Field
                  name="name"
                  label="Product name"
                  placeholder="HP EliteBook 840 G10"
                />
                <Field
                  name="category"
                  label="Category"
                  placeholder="Electronics"
                />
                <Field
                  name="unitPrice"
                  label="Unit price ($)"
                  placeholder="1049.99"
                  type="number"
                />
                <Field
                  name="description"
                  label="Description"
                  placeholder="Product details"
                />
              </>
            )}

            {modal === 'customer' && (
              <>
                <Field
                  name="name"
                  label="Contact name"
                  placeholder="Jordan Lee"
                />
                <Field
                  name="company"
                  label="Company"
                  placeholder="North Star Market"
                />
                <Field
                  name="email"
                  label="Email"
                  placeholder="orders@example.com"
                  type="email"
                />
                <Field
                  name="phone"
                  label="Phone"
                  placeholder="716-555-0100"
                />
              </>
            )}

            {modal === 'stock' && (
              <>
                {activeUnstockedProducts.length === 0 ? (
                  <p>
                    All active catalog products already have
                    inventory. Create another product in
                    Catalog first.
                  </p>
                ) : (
                  <>
                    <label className="field">
                      <span>Catalog product</span>
                      <select
                        name="sku"
                        required
                        defaultValue=""
                      >
                        <option value="" disabled>
                          Select an existing product
                        </option>

                        {activeUnstockedProducts.map(p => (
                          <option key={p.id} value={p.sku}>
                            {p.sku} — {p.name}
                          </option>
                        ))}
                      </select>
                    </label>

                    <Field
                      name="availableQuantity"
                      label="Available quantity"
                      placeholder="20"
                      type="number"
                    />
                    <Field
                      name="reorderLevel"
                      label="Reorder level"
                      placeholder="5"
                      type="number"
                    />
                    <Field
                      name="warehouse"
                      label="Warehouse"
                      placeholder="Buffalo"
                    />
                  </>
                )}
              </>
            )}

            {modal === 'edit-stock' && editingStock && (
              <>
                <label className="field">
                  <span>Catalog product</span>
                  <input
                    value={
                      `${editingStock.sku} — ${
                        products.find(
                          p => p.sku === editingStock.sku
                        )?.name || editingStock.productName
                      }`
                    }
                    readOnly
                  />
                </label>

                <label className="field">
                  <span>Available quantity</span>
                  <input
                    name="availableQuantity"
                    type="number"
                    min="0"
                    required
                    defaultValue={editingStock.availableQuantity}
                  />
                </label>

                <label className="field">
                  <span>Reorder level</span>
                  <input
                    name="reorderLevel"
                    type="number"
                    min="0"
                    required
                    defaultValue={editingStock.reorderLevel}
                  />
                </label>

                <label className="field">
                  <span>Warehouse</span>
                  <input
                    name="warehouse"
                    required
                    defaultValue={editingStock.warehouse}
                  />
                </label>

                <small>
                  Reserved quantity: {editingStock.reservedQuantity}.
                  Editing available stock does not change reservations.
                </small>
              </>
            )}

            {modal === 'order' && (
              <>
                <label className="field">
                  <span>Customer</span>
                  <select
                    name="customerId"
                    required
                    defaultValue=""
                  >
                    <option value="" disabled>
                      Select customer
                    </option>

                    {customers.map(c => (
                      <option value={c.id} key={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Product</span>
                  <select
                    name="sku"
                    required
                    defaultValue=""
                  >
                    <option value="" disabled>
                      Select product
                    </option>

                    {products.map(p => (
                      <option value={p.sku} key={p.id}>
                        {p.name} · {money(p.unitPrice)}
                      </option>
                    ))}
                  </select>
                </label>

                <Field
                  name="quantity"
                  label="Quantity"
                  placeholder="1"
                  type="number"
                />
              </>
            )}

            <div className="modal-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={closeModal}
              >
                Cancel
              </button>

              <button
                className="btn-primary"
                type="submit"
                disabled={
                  modal === 'stock' &&
                  activeUnstockedProducts.length === 0
                }
              >
                <Plus size={15} />
                Save
              </button>
            </div>
          </form>
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}

function Metric({
  label, value, change, icon: Icon, tone
}: {
  label: string
  value: string
  change: string
  icon: any
  tone: string
}) {
  return (
    <div className="metric-card">
      <div className="metric-top">
        <span>{label}</span>
        <div className={`metric-icon ${tone}`}>
          <Icon size={19} />
        </div>
      </div>
      <div className="metric-value">{value}</div>
      <div className="metric-foot">
        <span className="metric-neutral">
          <ArrowUpRight size={14} />
          {change}
        </span>
      </div>
    </div>
  )
}

function Toolbar({
  query, setQuery, placeholder
}: {
  query: string
  setQuery: (v: string) => void
  placeholder: string
}) {
  return (
    <div className="toolbar">
      <div className="search-box">
        <Search size={16} />
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder={placeholder}
        />
      </div>
      <span className="toolbar-note">Live workspace data</span>
    </div>
  )
}

function Status({ value }: { value: string }) {
  const tone =
    value === 'LOW STOCK' ? 'warning'
      : ['HEALTHY', 'IN STOCK', 'ACTIVE', 'DELIVERED'].includes(value)
        ? 'good'
        : value === 'CANCELLED'
          ? 'muted'
          : 'info'

  return (
    <span className={`status ${tone}`}>
      <i />
      {value}
    </span>
  )
}

function OrdersTable({
  orders, updateStatus, full = false
}: {
  orders: Order[]
  updateStatus: (id: number, s: string) => void
  full?: boolean
}) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ORDER</th>
            <th>CUSTOMER</th>
            <th>DATE</th>
            <th>ITEMS</th>
            <th>VALUE</th>
            <th>STATUS</th>
            {full && <th>UPDATE</th>}
          </tr>
        </thead>
        <tbody>
          {orders.map(o => (
            <tr key={o.id}>
              <td className="mono">
                #{String(o.id).padStart(5, '0')}
              </td>
              <td><b>{o.customerName}</b></td>
              <td>
                {new Date(o.createdAt).toLocaleDateString()}
              </td>
              <td>
                {o.items.length} line item
                {o.items.length === 1 ? '' : 's'}
              </td>
              <td className="strong">
                {money(o.total)}
              </td>
              <td>
                <Status value={o.status} />
              </td>
              {full && (
                <td>
                  <select
                    className="status-select"
                    value={o.status}
                    onChange={e =>
                      updateStatus(o.id, e.target.value)
                    }
                  >
                    <option>PLACED</option>
                    <option>PROCESSING</option>
                    <option>SHIPPED</option>
                    <option>DELIVERED</option>
                    <option>CANCELLED</option>
                  </select>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      {orders.length === 0 && (
        <Empty label="No orders yet" />
      )}
    </div>
  )
}

function ActivityList({ notices }: { notices: Notice[] }) {
  return (
    <div className="activity-list">
      {notices.map(n => (
        <div className="activity-row" key={n.id}>
          <div className="activity-dot">
            <ClipboardList size={15} />
          </div>
          <div className="activity-copy">
            <b>{n.message}</b>
            <small>
              {n.type} · {
                n.createdAt
                  ? new Date(n.createdAt).toLocaleString()
                  : ''
              }
            </small>
          </div>
          <span className="activity-ref">
            {n.referenceId || '—'}
          </span>
        </div>
      ))}
    </div>
  )
}

function Empty({ label }: { label: string }) {
  return (
    <div className="empty-state">
      <div className="empty-icon">
        <Package size={22} />
      </div>
      <b>{label}</b>
      <span>Create a record to get started.</span>
    </div>
  )
}

function Field({
  name, label, placeholder, type = 'text'
}: {
  name: string
  label: string
  placeholder: string
  type?: string
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input
        name={name}
        type={type}
        placeholder={placeholder}
        required
      />
    </label>
  )
}

export default App

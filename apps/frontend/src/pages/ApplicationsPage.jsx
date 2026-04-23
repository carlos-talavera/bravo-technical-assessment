import { useCallback, useEffect, useRef, useState } from "react";
import { api, createEventSource } from "../api";
import StatusBadge from "../components/StatusBadge";

const COUNTRIES = ["MX", "CO"];
const STATUSES = ["PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED"];

const STATUS_LABELS = {
  PENDING: "Pendiente",
  UNDER_REVIEW: "En revisión",
  APPROVED: "Aprobada",
  REJECTED: "Rechazada",
};

const COUNTRY_LABELS = { MX: "México", CO: "Colombia" };

const TRANSITIONS = {
  PENDING: ["UNDER_REVIEW"],
  UNDER_REVIEW: [],
  APPROVED: [],
  REJECTED: [],
};

const DOC_PLACEHOLDER = { MX: "CURP", CO: "Cédula de ciudadanía" };

function fmt(amount, country) {
  const currency = country === "MX" ? "MXN" : "COP";
  return new Intl.NumberFormat("es-MX", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

function fmtDate(iso) {
  return new Date(iso).toLocaleString("es-MX", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export default function ApplicationsPage({ token, onLogout }) {
  const [filter, setFilter] = useState({ country: "MX", status: "PENDING" });
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [sseConnected, setSseConnected] = useState(false);

  const filterRef = useRef(filter);
  useEffect(() => {
    filterRef.current = filter;
  }, [filter]);

  // SSE
  useEffect(() => {
    const es = createEventSource(token);

    es.onopen = () => setSseConnected(true);
    es.onerror = () => setSseConnected(false);

    es.addEventListener("application-update", (e) => {
      const updated = JSON.parse(e.data);
      const { country, status } = filterRef.current;

      setApplications((prev) => {
        const exists = prev.some((a) => a.id === updated.id);
        const matchFilter =
          updated.country === country && updated.status === status;

        if (exists && matchFilter) {
          // Actualiza
          return prev.map((a) => (a.id === updated.id ? updated : a));
        }
        if (exists && !matchFilter) {
          // Elimina (ya no tiene ese estatus)
          return prev.filter((a) => a.id !== updated.id);
        }
        // Añade
        if (!exists && matchFilter) return [updated, ...prev];
        return prev;
      });

      setSelected((prev) => (prev?.id === updated.id ? updated : prev));
    });

    return () => es.close();
  }, [token]);

  // Cargar lista
  const loadApplications = useCallback(
    async (f = filter) => {
      setLoading(true);
      setError("");
      try {
        const data = await api.listApplications(f.country, f.status);
        setApplications(data);
        setSelected(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    },
    [filter]
  );

  useEffect(() => {
    loadApplications();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function handleFilterChange(key, value) {
    const next = { ...filter, [key]: value };
    setFilter(next);
    loadApplications(next);
  }

  return (
    <>
      {/* ── Barra de navegación ── */}
      <nav className="navbar">
        <span className="navbar-brand">Bravo Credit</span>
        <div className="navbar-right">
          <span
            className={`sse-dot${sseConnected ? " connected" : ""}`}
            title={
              sseConnected
                ? "Tiempo real activo"
                : "Sin conexión en tiempo real"
            }
          />
          <button className="btn btn-secondary" onClick={onLogout}>
            Cerrar sesión
          </button>
        </div>
      </nav>

      <div className="page">
        {/* ── Barra de filtros ── */}
        <div className="filter-bar">
          <div className="field">
            <label>País</label>
            <select
              value={filter.country}
              onChange={(e) => handleFilterChange("country", e.target.value)}
            >
              {COUNTRIES.map((c) => (
                <option key={c} value={c}>
                  {COUNTRY_LABELS[c]}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>Estado</label>
            <select
              value={filter.status}
              onChange={(e) => handleFilterChange("status", e.target.value)}
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>

          <button
            className="btn btn-primary"
            onClick={() => setShowCreate(true)}
          >
            + Nueva solicitud
          </button>
        </div>

        {/* ── Lista de aplicaciones── */}
        {loading && <p className="state-box">Cargando…</p>}
        {error && (
          <p className="state-box" style={{ color: "var(--danger)" }}>
            {error}
          </p>
        )}

        {!loading && !error && applications.length === 0 && (
          <p className="state-box">No hay solicitudes para este filtro.</p>
        )}

        {!loading && !error && applications.length > 0 && (
          <div className="cards-grid">
            {applications.map((app) => (
              <div
                key={app.id}
                className={`app-card${selected?.id === app.id ? " selected" : ""}`}
                onClick={() => setSelected(app)}
              >
                <div className="app-card-top">
                  <div>
                    <div className="app-card-name">{app.fullName}</div>
                    <div className="app-card-meta">
                      {COUNTRY_LABELS[app.country]} · {app.documentId}
                    </div>
                  </div>
                  <StatusBadge status={app.status} />
                </div>
                <div className="app-card-amount">
                  Solicitado: {fmt(app.requestedAmount, app.country)}
                </div>
              </div>
            ))}
          </div>
        )}

        {selected && (
          <DetailPanel
            application={selected}
            onClose={() => setSelected(null)}
          />
        )}
      </div>

      {showCreate && <CreateModal onClose={() => setShowCreate(false)} />}
    </>
  );
}

// ── Panel de detalle ──────────────────────────────────────────────────────────────

function DetailPanel({ application: app, onClose }) {
  const nextStatuses = TRANSITIONS[app.status] ?? [];
  const [newStatus, setNewStatus] = useState(nextStatuses[0] ?? "");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleUpdate() {
    if (!newStatus) return;
    setLoading(true);
    setError("");
    try {
      await api.updateStatus(app.id, newStatus);
      // SSE actualizará el panel automáticamente a través de setSelected en el padre
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="detail-panel">
      <h2>Detalle de solicitud</h2>

      <div className="detail-grid">
        <div className="detail-item">
          <span className="detail-label">Nombre</span>
          <span className="detail-value">{app.fullName}</span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Estado</span>
          <span className="detail-value">
            <StatusBadge status={app.status} />
          </span>
        </div>
        <div className="detail-item">
          <span className="detail-label">País</span>
          <span className="detail-value">{COUNTRY_LABELS[app.country]}</span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Documento</span>
          <span className="detail-value">{app.documentId}</span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Monto solicitado</span>
          <span className="detail-value">
            {fmt(app.requestedAmount, app.country)}
          </span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Ingreso mensual</span>
          <span className="detail-value">
            {fmt(app.monthlyIncome, app.country)}
          </span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Banco</span>
          <span className="detail-value">
            {app.bankName} ({app.bankCurrency})
          </span>
        </div>
        <div className="detail-item">
          <span className="detail-label">Actualizada</span>
          <span className="detail-value">{fmtDate(app.updatedAt)}</span>
        </div>
      </div>

      <div className="detail-actions">
        {nextStatuses.length > 0 ? (
          <>
            <div className="field" style={{ marginBottom: 0, minWidth: 160 }}>
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
              >
                {nextStatuses.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
            </div>
            <button
              className="btn btn-primary"
              onClick={handleUpdate}
              disabled={loading}
            >
              {loading ? "Guardando…" : "Actualizar estado"}
            </button>
          </>
        ) : null}

        {error && (
          <span className="error-msg" style={{ marginBottom: 0 }}>
            {error}
          </span>
        )}

        <button
          className="btn btn-secondary"
          style={{ marginLeft: "auto" }}
          onClick={onClose}
        >
          Cerrar
        </button>
      </div>
    </div>
  );
}

// ── Modal para creación ──────────────────────────────────────────────────────────────

function CreateModal({ onClose }) {
  const [form, setForm] = useState({
    country: "MX",
    fullName: "",
    documentId: "",
    requestedAmount: "",
    monthlyIncome: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await api.createApplication({
        country: form.country,
        fullName: form.fullName,
        documentId: form.documentId,
        requestedAmount: parseFloat(form.requestedAmount),
        monthlyIncome: parseFloat(form.monthlyIncome),
      });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="modal-backdrop"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="modal">
        <h2>Nueva solicitud de crédito</h2>

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>País</label>
            <select
              value={form.country}
              onChange={(e) => set("country", e.target.value)}
            >
              {COUNTRIES.map((c) => (
                <option key={c} value={c}>
                  {COUNTRY_LABELS[c]}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label>Nombre completo</label>
            <input
              type="text"
              value={form.fullName}
              onChange={(e) => set("fullName", e.target.value)}
              placeholder="Nombre completo del solicitante"
              required
            />
          </div>

          <div className="field">
            <label>Documento de identidad</label>
            <input
              type="text"
              value={form.documentId}
              onChange={(e) => set("documentId", e.target.value)}
              placeholder={DOC_PLACEHOLDER[form.country]}
              required
            />
          </div>

          <div className="field">
            <label>Monto solicitado</label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.requestedAmount}
              onChange={(e) => set("requestedAmount", e.target.value)}
              placeholder="0.00"
              required
            />
          </div>

          <div className="field">
            <label>Ingreso mensual</label>
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.monthlyIncome}
              onChange={(e) => set("monthlyIncome", e.target.value)}
              placeholder="0.00"
              required
            />
          </div>

          {error && <p className="error-msg">{error}</p>}

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? "Creando…" : "Crear solicitud"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

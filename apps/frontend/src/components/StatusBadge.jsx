const LABELS = {
  PENDING:      'Pendiente',
  UNDER_REVIEW: 'En revisión',
  APPROVED:     'Aprobada',
  REJECTED:     'Rechazada',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`badge badge-${status}`}>
      {LABELS[status] ?? status}
    </span>
  )
}

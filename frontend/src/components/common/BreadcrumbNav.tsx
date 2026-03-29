import { memo, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';

interface BreadcrumbNavItem {
  label: string;
  path?: string; // undefined → 현재 페이지 (클릭 불가)
}

export const BreadcrumbNav = memo(function BreadcrumbNav({ items }: { items: BreadcrumbNavItem[] }) {
  const navigate = useNavigate();
  return (
    <nav className="breadcrumb-nav" aria-label="breadcrumb">
      <ol style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', listStyle: 'none', margin: 0, padding: 0, gap: 4 }}>
        {items.map((item, i) => (
          <Fragment key={item.path ?? `current-${item.label}`}>
            <li>
              {item.path ? (
                <button className="breadcrumb-btn" onClick={() => navigate(item.path!)}>
                  {item.label}
                </button>
              ) : (
                <span aria-current="page">{item.label}</span>
              )}
            </li>
            {i < items.length - 1 && (
              <li aria-hidden="true" style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>
                /
              </li>
            )}
          </Fragment>
        ))}
      </ol>
    </nav>
  );
});

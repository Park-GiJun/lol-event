import { Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '../ui/breadcrumb';

interface BreadcrumbNavItem {
  label: string;
  path?: string; // undefined → 현재 페이지 (클릭 불가)
}

export function BreadcrumbNav({ items }: { items: BreadcrumbNavItem[] }) {
  const navigate = useNavigate();
  return (
    <Breadcrumb className="breadcrumb-nav">
      <BreadcrumbList>
        {items.map((item, i) => (
          <Fragment key={i}>
            <BreadcrumbItem>
              {item.path ? (
                <BreadcrumbLink asChild>
                  <button className="breadcrumb-btn" onClick={() => navigate(item.path!)}>
                    {item.label}
                  </button>
                </BreadcrumbLink>
              ) : (
                <BreadcrumbPage>{item.label}</BreadcrumbPage>
              )}
            </BreadcrumbItem>
            {i < items.length - 1 && <BreadcrumbSeparator />}
          </Fragment>
        ))}
      </BreadcrumbList>
    </Breadcrumb>
  );
}

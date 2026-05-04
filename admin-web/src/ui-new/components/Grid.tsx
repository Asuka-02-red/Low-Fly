import React from 'react';
import styled from 'styled-components';

export const Container = styled.div`
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding-left: var(--space-6, 24px);
  padding-right: var(--space-6, 24px);
  box-sizing: border-box;
`;

export const Row = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-left: calc(var(--space-4, 16px) * -0.5);
  margin-right: calc(var(--space-4, 16px) * -0.5);
`;

interface ColProps {
  span?: number; // 1-12
  sm?: number;
  md?: number;
  lg?: number;
  xl?: number;
}

export const Col = styled.div<ColProps>`
  box-sizing: border-box;
  padding-left: calc(var(--space-4, 16px) * 0.5);
  padding-right: calc(var(--space-4, 16px) * 0.5);
  width: ${({ span }) => (span ? `${(span / 12) * 100}%` : '100%')};

  @media (min-width: 576px) {
    width: ${({ sm, span }) => (sm ? `${(sm / 12) * 100}%` : span ? `${(span / 12) * 100}%` : '100%')};
  }

  @media (min-width: 768px) {
    width: ${({ md, sm, span }) => (md ? `${(md / 12) * 100}%` : sm ? `${(sm / 12) * 100}%` : span ? `${(span / 12) * 100}%` : '100%')};
  }

  @media (min-width: 992px) {
    width: ${({ lg, md, sm, span }) => (lg ? `${(lg / 12) * 100}%` : md ? `${(md / 12) * 100}%` : sm ? `${(sm / 12) * 100}%` : span ? `${(span / 12) * 100}%` : '100%')};
  }
`;

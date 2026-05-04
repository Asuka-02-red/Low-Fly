import React from 'react';
import styled, { css } from 'styled-components';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isFullWidth?: boolean;
}

const getVariantStyles = (variant: ButtonProps['variant']) => {
  switch (variant) {
    case 'secondary':
      return css`
        background-color: var(--color-bg);
        color: var(--color-secondary);
        border: 1px solid var(--color-secondary);
        &:hover:not(:disabled) {
          background-color: rgba(23, 43, 77, 0.04);
        }
      `;
    case 'danger':
      return css`
        background-color: var(--color-danger);
        color: #FFFFFF;
        border: 1px solid var(--color-danger);
        &:hover:not(:disabled) {
          filter: brightness(0.92);
        }
      `;
    case 'primary':
    default:
      return css`
        background-color: var(--color-primary);
        color: #FFFFFF;
        border: 1px solid var(--color-primary);
        &:hover:not(:disabled) {
          filter: brightness(0.92);
          box-shadow: 0 4px 12px rgba(0, 82, 204, 0.2);
        }
      `;
  }
};

const getSizeStyles = (size: ButtonProps['size']) => {
  switch (size) {
    case 'sm':
      return css`
        padding: var(--space-2) var(--space-3);
        font-size: var(--text-sm);
      `;
    case 'lg':
      return css`
        padding: var(--space-4) var(--space-6);
        font-size: var(--text-lg);
      `;
    case 'md':
    default:
      return css`
        padding: var(--space-3) var(--space-4);
        font-size: var(--text-base);
      `;
  }
};

const StyledButton = styled.button<ButtonProps>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  font-weight: 500;
  border-radius: 4px;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
  width: ${({ isFullWidth }) => (isFullWidth ? '100%' : 'auto')};

  /* 触摸目标优化 (Mobile) */
  @media (max-width: 576px) {
    min-height: 48px;
  }

  ${({ variant }) => getVariantStyles(variant)}
  ${({ size }) => getSizeStyles(size)}

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const Button: React.FC<ButtonProps> = ({ children, ...props }) => {
  return <StyledButton {...props}>{children}</StyledButton>;
};
